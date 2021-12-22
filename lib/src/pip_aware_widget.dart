import 'package:flutter/material.dart';

import 'flutter_pip.dart';
import 'package:flutter/foundation.dart';

// class PipAwareWidget extends StatelessWidget {
//   PipAwareWidget({
//     Key key,
//     @required this.builder,
//     @required this.pipBuilder,
//   }) : super(key: key);

//   final WidgetBuilder builder;
//   final WidgetBuilder pipBuilder;

//   @override
//   Widget build(BuildContext context) {
//     return StreamBuilder<bool>(
//       stream: FlutterPip.onPiPModeChanged,
//       builder: (context, snapshot) {
//         if (snapshot.hasData && snapshot.data) {
//           return pipBuilder(context);
//         }
//         return builder(context);
//       },
//     );
//   }
// }

class PipWidget extends StatefulWidget {
  final Widget? child;
  final Function(bool)? onResume;
  final void Function()? onSuspending;
  PipWidget({this.child, this.onResume, this.onSuspending});
  @override
  _PipWidgetState createState() => _PipWidgetState();
}

class _PipWidgetState extends State<PipWidget> with WidgetsBindingObserver {
  WidgetsBindingObserver? observer;
  @override
  void initState() {
    observer = LifecycleEventHandler(resumeCallBack: () async {
      var isInPipMode = await FlutterPip.getchannelavailable();
      widget.onResume!(isInPipMode);
      return;
    }, suspendingCallBack: () {
      widget.onSuspending!();
    });
    super.initState();
    WidgetsBinding.instance!.addObserver(observer!);
  }

  @override
  void dispose() {
    WidgetsBinding.instance!.removeObserver(observer!);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return widget.child!;
  }
}

class LifecycleEventHandler extends WidgetsBindingObserver {
  final AsyncCallback? resumeCallBack;
  final VoidCallback? suspendingCallBack;

  LifecycleEventHandler({this.resumeCallBack, this.suspendingCallBack});

  @override
  Future<Null> didChangeAppLifecycleState(AppLifecycleState state) async {
    switch (state) {
      case AppLifecycleState.resumed:
        if (resumeCallBack != null) {
          await resumeCallBack!();
        }
        break;
      case AppLifecycleState.inactive:
      case AppLifecycleState.paused:
      case AppLifecycleState.detached:
        if (suspendingCallBack != null) {
          suspendingCallBack!();
        }
        break;
    }
  }
}
