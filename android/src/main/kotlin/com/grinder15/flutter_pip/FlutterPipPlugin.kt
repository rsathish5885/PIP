package com.grinder15.flutter_pip

import android.app.PictureInPictureParams
import android.os.Build
import android.util.Log
import android.util.Rational
import androidx.annotation.NonNull
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import android.os.Handler
import android.os.Looper

import android.annotation.SuppressLint

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.embedding.engine.plugins.lifecycle.FlutterLifecycleAdapter
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar

/** FlutterPipPlugin */
class FlutterPipPlugin : FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler,
        ActivityAware, PluginRegistry.UserLeaveHintListener, LifecycleObserver {
    private val TAG: String = "FlutterPipPlugin"
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private lateinit var closeeventChannel: EventChannel
    private var activityPluginBinding: ActivityPluginBinding? = null
    private var eventSink: EventChannel.EventSink? = null
    
     val connectionStreamHandler = ConnectionStreamHandler()
    private var isInPipMode: Boolean? = null
    private var pipReady: Boolean = false
    private var numerator: Double? = null
    private var denominator: Double? = null
   

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, FLUTTER_PIP)
        channel.setMethodCallHandler(this)
        eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, FLUTTER_PIP_EVENT)
        eventChannel.setStreamHandler(this)
        closeeventChannel = EventChannel(flutterPluginBinding.binaryMessenger, CLOSE_PIP_EVENT)
        closeeventChannel.setStreamHandler(this.connectionStreamHandler)
    }

    // This static function is optional and equivalent to onAttachedToEngine. It supports the old
    // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
    // plugin registration via this function while apps migrate to use the new Android APIs
    // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
    //
    // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
    // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
    // depending on the user's project. onAttachedToEngine or registerWith must both be defined
    // in the same class.
    companion object {
        const val FLUTTER_PIP: String = "flutter_pip"
        const val FLUTTER_PIP_EVENT: String = "flutter_pip_event"
        const val CLOSE_PIP_EVENT: String = "close_pip_event"
        const val SET_PIP_READY: String = "setPiPReady"
        const val UNSET_PIP_READY: String = "unsetPiPReady"
        const val GET_PIP_READY: String = "getPiPReadyStatus"
        const val SWITCH_TO_PIP_MODE: String = "switchToPiPMode"
        const val GETSTATUSAVAILABLE: String = "isavailable"
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val plugin = FlutterPipPlugin()
            val channel = MethodChannel(registrar.messenger(), FLUTTER_PIP)
            channel.setMethodCallHandler(plugin)
            val eventChannel = EventChannel(registrar.messenger(), FLUTTER_PIP_EVENT)
            eventChannel.setStreamHandler(plugin)
            val closeeventChannel = EventChannel(registrar.messenger(), CLOSE_PIP_EVENT)
            closeeventChannel.setStreamHandler(plugin)
           
        }
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        if (call.method == SWITCH_TO_PIP_MODE) {
            if (checkIfPiPSupported()) {
                if (getPiPReady()) {
                    switchToPiPMode(numerator, denominator)
                    result.success(null)
                } else {
                    result.error("WRONG_CONFIGURATION", "You're trying to go Pip mode without setup. To use PiP Mode, call \"setPipReady\" method and supply numerator and denominator arguments", null)
                }
            } else {
                result.error("NOT_SUPPORTED", "PiP Mode in SDK ${Build.VERSION.SDK_INT} is not supported.", null)
            }
        } else if (call.method == SET_PIP_READY) {
            if (checkIfPiPSupported()) {
                result.success(setPiPReady(call.argument<Double>("numerator"),
                        call.argument<Double>("denominator")))
                      
            } else {
                // result.error("NOT_SUPPORTED", "PiP Mode in SDK ${Build.VERSION.SDK_INT} is not supported.", null)
                result.success(1);
            }
        } else if (call.method == UNSET_PIP_READY) {
            var isInPictureInPictureMode: Boolean? = false
      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
        isInPictureInPictureMode = activityPluginBinding?.activity?.isInPictureInPictureMode
      }
      if (isInPictureInPictureMode==true) {
        activityPluginBinding?.activity?.moveTaskToBack(true)
        // android.os.Process.killProcess(android.os.Process.myPid())
      }
      result.success(isInPictureInPictureMode);
    }
         else if (call.method == GET_PIP_READY) {
            result.success(getPiPReady())
        } 
        else if (call.method == GETSTATUSAVAILABLE) {
            var isavailableornotoutside: Boolean? = false
      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
      
         isavailableornotoutside = activityPluginBinding?.activity?.isInPictureInPictureMode
      }
      result.success(isavailableornotoutside);
        } 
        
        else {

            result.notImplemented()
        }
    }

    class ConnectionStreamHandler : EventChannel.StreamHandler {
        private var closeeventSink: EventChannel.EventSink? = null
        override fun onListen(arguments: Any?, sink: EventChannel.EventSink) {
            closeeventSink = sink
        }
    
        fun send(code : Boolean) {
           
            Handler(Looper.getMainLooper()).post {
                closeeventSink?.success(code)
            }
    
        }
    
        override fun onCancel(p0: Any?) {
            closeeventSink = null
        }
    }



    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events
       

        checkActivityPictureInPictureMode()
    }
   

    override fun onCancel(arguments: Any?) {
        eventSink = null
       
       
    }

    private fun checkIfPiPSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

    private fun switchToPiPMode(numerator: Double?, denominator: Double?) {
        if (checkIfPiPSupported()) {
            if (denominator != null && numerator != null) {
                val pipParams = PictureInPictureParams.Builder()
                        //.setAspectRatio(Rational(width + (width * 0.6).toInt(), height))
                        .setAspectRatio(Rational(numerator.toInt(), denominator.toInt()))
                        .build()
                activityPluginBinding?.activity?.enterPictureInPictureMode(pipParams)
            }
        }
    }

    private fun setPiPReady(numerator: Double?, denominator: Double?): Int {
       

        if (checkIfPiPSupported()) {
            if (denominator != null && numerator != null) {
                val pipParams = PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(numerator.toInt(), denominator.toInt()))
                        .build()
                activityPluginBinding?.activity?.enterPictureInPictureMode(pipParams)
            }
        }
        return 0
    }

    private fun unsetPiPReady(): Boolean {
        this.pipReady = false
        this.numerator = null
        this.denominator = null
        return pipReady
    }
  

    private fun getPiPReady(): Boolean {
        return pipReady && numerator != null && denominator != null
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
    }

    override fun onDetachedFromActivity() {
        unSetActivityBinding()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        setActivityBinding(binding)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        setActivityBinding(binding)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        unSetActivityBinding()
    }

    private fun setActivityBinding(binding: ActivityPluginBinding) {
        activityPluginBinding = binding
        if (activityPluginBinding != null) {
            FlutterLifecycleAdapter.getActivityLifecycle(activityPluginBinding!!).addObserver(this)
            activityPluginBinding?.addOnUserLeaveHintListener(this)
           
        }
    }

    private fun unSetActivityBinding() {
        if (activityPluginBinding != null) {
            FlutterLifecycleAdapter.getActivityLifecycle(activityPluginBinding!!).removeObserver(this)
            activityPluginBinding?.removeOnUserLeaveHintListener(this)
           
        }
        activityPluginBinding = null
    }

    override fun onUserLeaveHint() {
       
        if (getPiPReady()) {
            switchToPiPMode(numerator, denominator)
        }
     
       
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun onActivityResume() {
      
        checkActivityPictureInPictureMode()
       
        connectionStreamHandler.send(false)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private fun onActivityPause() {
       
        checkActivityPictureInPictureMode()
       
        connectionStreamHandler.send(false)
     
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onActivityStop() {
        println("Hello, World! onActivityStop.....")
        checkActivityPictureInPictureMode()
      
    
        
        connectionStreamHandler.send(true)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onActivityDestroy() {
       
        checkActivityPictureInPictureMode()
       
      
        connectionStreamHandler.send(false)
    }
    
    private fun checkActivityPictureInPictureMode() {

        if (eventSink != null) {
            if (activityPluginBinding != null) {
                if (checkIfPiPSupported()) {
                    val isPiPMode = activityPluginBinding?.activity?.isInPictureInPictureMode
                    // TODO: isPipChangeEvent always triggered because we hook it up in onResume and onPause
                    // later I wish there is a way to override activity callbacks especially "onPictureInPictureModeChanged()"
                    // callback to prevent sending redundant events.
                    if (this.isInPipMode != isPiPMode) {
                        eventSink?.success(isPiPMode)
                        println("Hello, World! if......")
                    }
                } else {
                    eventSink?.error(
                            "NOT_SUPPORTED",
                            "PiP Mode in SDK ${Build.VERSION.SDK_INT} is not supported.",
                            null)
                        println("Hello, World! else")
                }
            }
            else 
            {
                Log.i(TAG, "onListen activityPluginBinding ....null.....")
            }
        }
        else
        {
            Log.i(TAG, "onListen Closed.....")
        }
    }



    // private fun isAvailableorNot() {

    //     if (eventSink != null) {
    //         if (activityPluginBinding != null) {
    //             if (checkIfPiPSupported()) {
    //                 val isPiPMode = activityPluginBinding?.activity?.isInPictureInPictureMode
    //                 // TODO: isPipChangeEvent always triggered because we hook it up in onResume and onPause
    //                 // later I wish there is a way to override activity callbacks especially "onPictureInPictureModeChanged()"
    //                 // callback to prevent sending redundant events.
    //                 if (this.isInPipMode != isPiPMode) {
    //                     eventSink?.success(isPiPMode)
    //                     println("Hello, World! if......")
    //                 }
    //             } else {
    //                 eventSink?.error(
    //                         "NOT_SUPPORTED",
    //                         "PiP Mode in SDK ${Build.VERSION.SDK_INT} is not supported.",
    //                         null)
    //                     println("Hello, World! else")
    //             }
    //         }
    //         else 
    //         {
    //             Log.i(TAG, "onListen activityPluginBinding ....null.....")
    //         }
    //     }
    //     else
    //     {
    //         Log.i(TAG, "onListen Closed.....")
    //     }
    // }
}
