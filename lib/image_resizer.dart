// You have generated a new plugin project without specifying the `--platforms`
// flag. A plugin project with no platform support was generated. To add a
// platform, run `flutter create -t plugin --platforms <platforms> .` under the
// same directory. You can also find a detailed instruction on how to add
// platforms in the `pubspec.yaml` at
// https://flutter.dev/docs/development/packages-and-plugins/developing-packages#plugin-platforms.

import 'dart:async';
import 'package:flutter/services.dart';

class ImageResizer {
  static const MethodChannel _channel = const MethodChannel('image_processor');

  static Future<String?> processImage({
    required String srcImagePath,
    required String outputImageName,
    int quality = 50,
  }) async {
    final Map<String, dynamic> params = {
      'srcImagePath': srcImagePath,
      'outputImageName': outputImageName,
      'quality': quality,
    };

    try {
      final String? result =
          await _channel.invokeMethod('processImage', params);
      return result;
    } on PlatformException catch (e) {
      return 'Error: ${e.message}';
    }
  }
}
