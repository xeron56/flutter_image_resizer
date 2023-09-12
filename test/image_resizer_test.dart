import 'package:flutter_test/flutter_test.dart';
import 'package:image_resizer/image_resizer.dart';
import 'package:image_resizer/image_resizer_platform_interface.dart';
import 'package:image_resizer/image_resizer_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockImageResizerPlatform
    with MockPlatformInterfaceMixin
    implements ImageResizerPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final ImageResizerPlatform initialPlatform = ImageResizerPlatform.instance;

  test('$MethodChannelImageResizer is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelImageResizer>());
  });

  test('getPlatformVersion', () async {
    ImageResizer imageResizerPlugin = ImageResizer();
    MockImageResizerPlatform fakePlatform = MockImageResizerPlatform();
    ImageResizerPlatform.instance = fakePlatform;

    expect(await imageResizerPlugin.getPlatformVersion(), '42');
  });
}
