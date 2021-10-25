
import 'dart:async';

import 'package:flutter/services.dart';

import '../models/pip_ratio.dart';

class FlutterPip {

  static const MethodChannel _channel =
      MethodChannel('flutter_pip');

  static Future<int?> enterPictureInPictureMode({PipRatio? pipRatio}) async {
    var ratio = pipRatio ?? PipRatio();
    // if (ratio.aspectRatio < 0.418410 || ratio.aspectRatio > 2.390000)
    //   throw PipRatioException.extremeRatio();

    return await _channel.invokeMethod('enterPictureInPictureMode',
      {'width': ratio.width,'height': ratio.height}
    );
  }

  static Future<bool?> isInPictureInPictureMode() async {
    return await _channel.invokeMethod('isInPictureInPictureMode');
  }

}