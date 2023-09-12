import 'dart:io';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:image_resizer/image_resizer.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: MyHomePage(),
    );
  }
}

class MyHomePage extends StatefulWidget {
  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  File? originalImage;
  File? compressedImage;
  int? originalSize;
  int? compressedSize;

  Future<void> pickImage(ImageSource source) async {
    final pickedFile = await ImagePicker().pickImage(source: source);

    if (pickedFile != null) {
      setState(() {
        originalImage = File(pickedFile.path);
        originalSize = originalImage!.lengthSync();
      });

      await compressImage();
    }
  }

  Future<void> compressImage() async {
    final outputImageName = 'output_image';
    final quality = 80;

    final result = await ImageResizer.processImage(
      srcImagePath: originalImage!.path,
      outputImageName: outputImageName,
      quality: quality,
    );

    setState(() {
      compressedImage = File(result!);
      compressedSize = compressedImage!.lengthSync();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Image Processing Example'),
      ),
      body: SingleChildScrollView(
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              originalImage != null
                  ? Column(
                      children: [
                        Image.file(originalImage!),
                        Text('Original Size: ${originalSize! / 1024} KB'),
                      ],
                    )
                  : Container(),
              SizedBox(height: 16),
              compressedImage != null
                  ? Column(
                      children: [
                        Image.file(compressedImage!),
                        Text('Compressed Size: ${compressedSize! / 1024} KB'),
                      ],
                    )
                  : Container(),
            ],
          ),
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          showModalBottomSheet(
            context: context,
            builder: (context) => Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                ListTile(
                  leading: Icon(Icons.camera_alt),
                  title: Text('Take a picture'),
                  onTap: () {
                    pickImage(ImageSource.camera);
                    Navigator.pop(context);
                  },
                ),
                ListTile(
                  leading: Icon(Icons.photo_library),
                  title: Text('Choose from gallery'),
                  onTap: () {
                    pickImage(ImageSource.gallery);
                    Navigator.pop(context);
                  },
                ),
              ],
            ),
          );
        },
        child: Icon(Icons.add),
      ),
    );
  }
}
