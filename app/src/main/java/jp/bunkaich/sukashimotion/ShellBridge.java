package jp.bunkaich.sukashimotion;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.*;
import android.hardware.HardwareBuffer;
import android.os.*;
import android.view.SurfaceControl;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/** Shizuku UserService: shell authority stays in this process, not in the UI. */
public final class ShellBridge extends IShellBridge.Stub {
    private final ScheduledExecutorService life=Executors.newSingleThreadScheduledExecutor();
    private final HandlerThread sensorThread=new HandlerThread("motion-sensors");
    private final List<SensorEventListener> listeners=new ArrayList<>();
    private final List<Bundle> sensors=new ArrayList<>();
    private Context context; private SensorManager sensorManager; private int appUid=-1;
    private volatile long heartbeat=SystemClock.elapsedRealtime();
    private volatile IAngleSink sink; private volatile java.lang.Process logReader; private volatile int angleGeneration;
    private String error=""; private DualDisplayControl displayControl;private TaskDisplayRouter taskRouter;private StatusBarControl bars;
    public ShellBridge() { this(null); }
    public ShellBridge(Context ignored) {
        try {
            Class<?> at=Class.forName("android.app.ActivityThread");
            Object thread=at.getMethod("currentActivityThread").invoke(null);
            if(thread==null)thread=at.getMethod("systemMain").invoke(null);
            Context system=(Context)at.getMethod("getSystemContext").invoke(thread);
            context=system.createPackageContext("com.android.shell",0);
            appUid=context.getPackageManager().getPackageUid(BuildConfig.APPLICATION_ID,0);
            sensorManager=context.getSystemService(SensorManager.class);
        } catch(Exception e) { error=message(e); }
        sensorThread.start();
        life.scheduleWithFixedDelay(()->{
            if(SystemClock.elapsedRealtime()-heartbeat>5000){ stopInternal(); releaseInternal(); }
        },1,1,TimeUnit.SECONDS);
    }
    private void authorize(){
        int caller=Binder.getCallingUid();
        if(caller!=appUid && caller!=android.os.Process.myUid())throw new SecurityException("@folduo/err_unexpected_caller");
    }
    static String message(Throwable e){
        while(e.getCause()!=null)e=e.getCause();
        return e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage());
    }
    @Override public Bundle inspect(){
        authorize(); long token=Binder.clearCallingIdentity();
        try {
            Bundle b=new Bundle();b.putInt("uid",android.os.Process.myUid());b.putString("model",Build.MODEL);
            b.putString("error",error);b.putBoolean("running",sink!=null);
            b.putBoolean("statusIconsHidden",bars!=null&&bars.hidden());
            b.putBoolean("samsungPermission",context!=null&&context.checkSelfPermission("com.samsung.permission.SSENSOR")==android.content.pm.PackageManager.PERMISSION_GRANTED);
            synchronized(sensors){ArrayList<Bundle> copy=new ArrayList<>();for(Bundle row:sensors)copy.add(new Bundle(row));b.putParcelableArrayList("sensors",copy);}
            try { b.putString("display",control().describe()); } catch(Exception e){b.putString("display",message(e));}
            return b;
        } finally { Binder.restoreCallingIdentity(token); }
    }
    @Override public void heartbeat(){authorize();heartbeat=SystemClock.elapsedRealtime();}
    @Override public synchronized void startAngles(IAngleSink callback){
        authorize();long token=Binder.clearCallingIdentity();
        try {
            stopInternal();heartbeat=SystemClock.elapsedRealtime();sink=callback;int generation=angleGeneration;
            callback.asBinder().linkToDeath(()->{if(sink==callback){stopInternal();releaseInternal();}},0);
            if(sensorManager!=null){
                Handler handler=new Handler(sensorThread.getLooper());
                synchronized(sensors){sensors.clear();}
                for(Sensor sensor:sensorManager.getSensorList(Sensor.TYPE_ALL)){
                    int type=sensor.getType();
                    if(type!=4&&type!=16&&type!=36&&type!=65686&&type!=65687&&type!=65688&&type!=65689&&type!=65690)continue;
                    Bundle row=new Bundle();row.putString("name",sensor.getName());row.putInt("type",type);row.putFloat("resolution",sensor.getResolution());row.putLong("events",0);
                    SensorEventListener listener=new SensorEventListener(){
                        public void onAccuracyChanged(Sensor s,int accuracy){}
                        public void onSensorChanged(SensorEvent event){
                            synchronized(sensors){row.putLong("events",row.getLong("events")+1);row.putLong("lastAt",SystemClock.elapsedRealtime());row.putFloatArray("values",event.values.clone());}
                            if(type==36||type==65686){
                                // A 90-degree public sensor is a posture source, never label it fine.
                                int source=type==65686?2:sensor.getResolution()<10?3:0;
                                emit(event.values[0],SystemClock.elapsedRealtime(),source,generation);
                            }
                        }
                    };
                    try{boolean ok=sensorManager.registerListener(listener,sensor,20000,0,handler);row.putBoolean("registered",ok);if(ok)listeners.add(listener);}
                    catch(Exception e){row.putString("error",message(e));}
                    synchronized(sensors){sensors.add(row);}
                }
            }
            startLogReader(generation);
        } catch(Exception e){error=message(e);} finally{Binder.restoreCallingIdentity(token);}
    }
    private void emit(float angle,long measuredAt,int source,int generation){
        IAngleSink target=sink;
        if(generation!=angleGeneration||target==null||!Float.isFinite(angle)||angle<0||angle>180)return;
        try{target.angle(angle,measuredAt,source);}catch(RemoteException e){stopInternal();releaseInternal();}
    }
    private void startLogReader(int generation){
        new Thread(()->{
            long started=System.currentTimeMillis();
            Pattern pattern=AngleLog.LINE;
            java.lang.Process process=null;
            try{
                process=new ProcessBuilder("logcat","-v","epoch","-T","1","-s","SprWallpaper|FoldInteractive:I","*:S").redirectErrorStream(true).start();
                synchronized(this){if(generation!=angleGeneration){process.destroy();return;}logReader=process;}
                try(BufferedReader reader=new BufferedReader(new InputStreamReader(process.getInputStream()))){
                    String line;while(generation==angleGeneration&&(line=reader.readLine())!=null){
                        Matcher m=pattern.matcher(line);if(!m.find())continue;
                        long wallTime=(long)(Double.parseDouble(m.group(1))*1000),age=System.currentTimeMillis()-wallTime;
                        if(wallTime<started||age< -50||age>600)continue;
                        emit(Float.parseFloat(m.group(2)),SystemClock.elapsedRealtime()-Math.max(0,age),1,generation);
                    }
                }
            }catch(Exception e){if(generation==angleGeneration)error=message(e);}
            finally{if(process!=null)process.destroy();}
        },"motion-angle-reader").start();
    }
    @Override public void stopAngles(){authorize();long token=Binder.clearCallingIdentity();try{stopInternal();}finally{Binder.restoreCallingIdentity(token);}}
    private synchronized void stopInternal(){
        ++angleGeneration;sink=null;
        if(logReader!=null){logReader.destroy();logReader=null;}
        if(sensorManager!=null)for(SensorEventListener listener:listeners)sensorManager.unregisterListener(listener);
        listeners.clear();
    }
    private synchronized DualDisplayControl control()throws Exception{if(displayControl==null)displayControl=new DualDisplayControl();return displayControl;}
    @Override public synchronized Bundle hold(boolean innerPrimary,int previousOwner){
        authorize();long token=Binder.clearCallingIdentity();Bundle result=new Bundle();
        try{if(sink==null)throw new IllegalStateException("@folduo/err_angle_stopped");control().hold(innerPrimary,previousOwner);result.putInt("ownerPid",android.os.Process.myPid());result.putBoolean("ok",true);}catch(Exception e){result.putString("error",message(e));}
        finally{Binder.restoreCallingIdentity(token);}return result;
    }
    @Override public synchronized Bundle moveApp(int sourceDisplayId,int targetDisplayId,boolean idle){
        authorize();long token=Binder.clearCallingIdentity();Bundle result=new Bundle();
        try{
            if(sink==null||displayControl==null||!displayControl.isOwned())throw new IllegalStateException("@folduo/err_control_stopped");
            if((sourceDisplayId!=0&&sourceDisplayId!=1)||(targetDisplayId!=0&&targetDisplayId!=1)||sourceDisplayId==targetDisplayId)throw new IllegalArgumentException("@folduo/err_distinct_displays");
            if(taskRouter==null)taskRouter=new TaskDisplayRouter();
            return taskRouter.move(sourceDisplayId,targetDisplayId,idle);
        }catch(Exception e){result.putString("error",message(e));return result;}
        finally{Binder.restoreCallingIdentity(token);}
    }
    @Override public void release(){authorize();long token=Binder.clearCallingIdentity();try{releaseInternal();}finally{Binder.restoreCallingIdentity(token);}}
    private synchronized void releaseInternal(){
        try{if(bars!=null)bars.hide(false);}catch(Exception e){error=message(e);}
        try{if(taskRouter!=null&&displayControl!=null&&displayControl.isOwned())taskRouter.restore();}catch(Exception e){error=message(e);}
        finally{if(displayControl!=null)displayControl.close();taskRouter=null;}
    }
    @Override public synchronized Bundle statusIcons(boolean hidden){
        authorize();long token=Binder.clearCallingIdentity();Bundle result=new Bundle();
        try{
            if(hidden&&(sink==null||displayControl==null||!displayControl.isOwned()))throw new IllegalStateException("@folduo/err_monitor_inactive");
            if(bars==null)bars=new StatusBarControl();bars.hide(hidden);result.putBoolean("ok",true);
        }catch(Exception e){result.putString("error",message(e));}finally{Binder.restoreCallingIdentity(token);}return result;
    }
    @Override public synchronized Bundle navigate(int displayId,int action,int taskId){
        authorize();long token=Binder.clearCallingIdentity();Bundle result=new Bundle();
        try{
            if(displayId!=1||sink==null||displayControl==null||!displayControl.isOwned())throw new IllegalStateException("@folduo/err_inner_unavailable");
            if(taskRouter==null)taskRouter=new TaskDisplayRouter();
            if(action==android.view.KeyEvent.KEYCODE_HOME){
                android.content.Intent home=new android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME);
                android.content.ComponentName preferred=home.resolveActivity(context.getPackageManager());
                taskRouter.showHome(displayId,preferred);
            }
            else if(action==InnerNavigation.SETTINGS)taskRouter.openSettings(context,displayId);
            else if(action==InnerNavigation.PREVIEW)result.putParcelable("preview",taskRouter.preview(taskId));
            else if(action==android.view.KeyEvent.KEYCODE_BACK){taskRouter.focusTop(displayId);NavigationInput.back(displayId);}
            else if(action==android.view.KeyEvent.KEYCODE_APP_SWITCH)result.putParcelableArrayList("apps",taskRouter.recentApps(context));
            else if(action==0&&taskId>=0)taskRouter.selectRecent(taskId,displayId);
            else throw new IllegalArgumentException("@folduo/err_unsupported_action");
            result.putBoolean("ok",true);
        }catch(Exception e){result.putString("error",message(e));}finally{Binder.restoreCallingIdentity(token);}return result;
    }
    @Override public synchronized Bundle launchApp(int displayId,String component){
        authorize();long token=Binder.clearCallingIdentity();Bundle result=new Bundle();
        try{
            if(displayId!=1)throw new IllegalArgumentException("@folduo/err_launch_target");
            if(displayControl==null||!displayControl.isOwned()){
                result.putBoolean("ok",true);result.putBoolean("handled",false);return result;
            }
            if(sink==null)throw new IllegalStateException("@folduo/err_inner_unavailable");
            if(taskRouter==null)taskRouter=new TaskDisplayRouter();
            taskRouter.launchApp(context,android.content.ComponentName.unflattenFromString(component),displayId);
            result.putBoolean("ok",true);result.putBoolean("handled",true);
        }catch(Exception e){result.putString("error",message(e));}
        finally{Binder.restoreCallingIdentity(token);}return result;
    }
    @Override public Bundle windowState(int displayId){
        authorize();long token=Binder.clearCallingIdentity();Bundle result=new Bundle();java.lang.Process process=null;
        try{
            process=new ProcessBuilder("dumpsys","window","visible-apps").start();
            java.lang.Process owned=process;
            ScheduledFuture<?> timeout=life.schedule(owned::destroy,1200,TimeUnit.MILLISECONDS);
            String dump;try(InputStream in=process.getInputStream()){dump=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}finally{timeout.cancel(false);}
            WindowReadiness.State state=WindowReadiness.parse(dump,displayId);
            result.putBoolean("ready",state.ready());result.putString("geometry",state.geometry());
        }catch(Exception e){result.putString("error",message(e));}
        finally{if(process!=null)process.destroy();Binder.restoreCallingIdentity(token);}return result;
    }
    @Override public Bundle capture(int displayId){return captureBehind(displayId,new SurfaceControl[0]);}
    @Override public Bundle captureBehind(int displayId,SurfaceControl[] exclude){
        authorize();long token=Binder.clearCallingIdentity();Bundle result=new Bundle();
        try{
            // Never request secure/protected layers. No pixels are written to storage or logs.
            String family="android.window.ScreenCapture";
            try{Class.forName(family+"$CaptureArgs");}catch(ClassNotFoundException changed){family="android.window.ScreenCaptureInternal";}
            Class<?> capture=Class.forName(family);
            Class<?> argsClass=Class.forName(family+"$CaptureArgs");
            Class<?> builderClass=Class.forName(family+"$CaptureArgs$Builder");
            Object builder=builderClass.getConstructor().newInstance();
            if(family.endsWith("Internal")){
                // New Samsung/Android builds use explicit policies. Reject protected frames.
                Class<?> policies=Class.forName("android.window.ScreenCapture$ScreenCaptureParams");
                builderClass.getMethod("setSecureContentPolicy",int.class).invoke(builder,policies.getField("SECURE_CONTENT_POLICY_THROW_EXCEPTION").getInt(null));
                builderClass.getMethod("setProtectedContentPolicy",int.class).invoke(builder,policies.getField("PROTECTED_CONTENT_POLICY_THROW_EXCEPTION").getInt(null));
                builderClass.getMethod("setIncludeSystemOverlays",boolean.class).invoke(builder,true);
            }else{
                builderClass.getMethod("setCaptureSecureLayers",boolean.class).invoke(builder,false);
                builderClass.getMethod("setAllowProtected",boolean.class).invoke(builder,false);
            }
            if(exclude!=null&&exclude.length>0)builderClass.getMethod("setExcludeLayers",SurfaceControl[].class).invoke(builder,(Object)exclude);
            Object args=builderClass.getMethod("build").invoke(builder);
            Object listener=capture.getMethod("createSyncCaptureListener").invoke(null);
            IBinder binder=(IBinder)Class.forName("android.os.ServiceManager").getMethod("getService",String.class).invoke(null,"window");
            Object wm=Class.forName("android.view.IWindowManager$Stub").getMethod("asInterface",IBinder.class).invoke(null,binder);
            Class.forName("android.view.IWindowManager").getMethod("captureDisplay",int.class,argsClass,Class.forName(family+"$ScreenCaptureListener")).invoke(wm,displayId,args,listener);
            Object buffer=Class.forName(family+"$SynchronousScreenCaptureListener").getMethod("getBuffer").invoke(listener);
            if(buffer==null)throw new IOException("@folduo/err_no_frame");
            Class<?> bufferClass=Class.forName(family+"$ScreenshotHardwareBuffer");
            HardwareBuffer hardware=(HardwareBuffer)bufferClass.getMethod("getHardwareBuffer").invoke(buffer);
            try{
                if((boolean)bufferClass.getMethod("containsSecureLayers").invoke(buffer))throw new SecurityException("@folduo/err_protected_frame");
                Bitmap bitmap=(Bitmap)bufferClass.getMethod("asBitmap").invoke(buffer);
                if(bitmap==null)throw new IOException("@folduo/err_empty_frame");
                result.putParcelable("frame",bitmap);result.putBoolean("ok",true);
            }finally{if(hardware!=null)hardware.close();}
        }catch(Exception e){result.putString("error",message(e));}finally{Binder.restoreCallingIdentity(token);}
        return result;
    }
    @Override public void destroy(){authorize();stopInternal();releaseInternal();life.shutdownNow();sensorThread.quitSafely();System.exit(0);}
}
