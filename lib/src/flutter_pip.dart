import 'package:flutter/services.dart';

class FlutterPip {
  static const String _CHANNEL_NAME = 'flutter_pip';
  static const String _EVENT_CHANNEL_NAME = 'flutter_pip_event';
  static const String _CLOSE_BUTTON_EVENT_CHANNEL_NAME = 'close_pip_event';
  static const String _SET_PIP_READY = 'setPiPReady';
  static const String _SET_PIP_READY_ARG_NUMERATOR = 'numerator';
  static const String _SET_PIP_READY_ARG_DENOMINATOR = 'denominator';
  static const String _UNSET_PIP_READY = 'unsetPiPReady';
  static const String _GET_PIP_READY = 'getPiPReadyStatus';
  static const String _SWITCH_TO_PIP_MODE = 'switchToPiPMode';
  static const String _IS_AVAILABLE_NOT = 'isavailable';
  static const MethodChannel _channel = const MethodChannel(_CHANNEL_NAME);
  static const EventChannel _eventChannel =
      const EventChannel(_EVENT_CHANNEL_NAME);

  static const EventChannel _closeeventChannel =
      const EventChannel(_CLOSE_BUTTON_EVENT_CHANNEL_NAME);

  static Future<int> setPipReady(double numerator, double denominator) async {
    return await _channel.invokeMethod(
      _SET_PIP_READY,
      {
        _SET_PIP_READY_ARG_NUMERATOR: numerator,
        _SET_PIP_READY_ARG_DENOMINATOR: denominator,
      },
    );
  }

  static Future<bool> unsetPipReady() async {
    return await _channel.invokeMethod(_UNSET_PIP_READY);
  }

  static Future<bool> getPipReady() async {
    return await _channel.invokeMethod(_GET_PIP_READY);
  }

  static Future<void> switchToPiPMode() async {
    await _channel.invokeMethod(_SWITCH_TO_PIP_MODE);
  }

  static Future<bool> getchannelavailable() async {
    return await _channel.invokeMethod(_IS_AVAILABLE_NOT);
  }

  static Stream<bool> get onPiPModeChanged =>
      _eventChannel.receiveBroadcastStream().map((event) => event as bool);
  // .distinct();

  static Stream<bool> get onClosePiPModetrigger =>
      _closeeventChannel.receiveBroadcastStream().map((event) => event as bool);
}
