package com.example.image_resizer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.Nullable

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding

import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import kotlin.math.roundToInt

/** ImageProcessorPlugin */
class ImageResizerPlugin : FlutterPlugin, MethodCallHandler {
  private lateinit var channel: MethodChannel
  private lateinit var context: Context

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "image_processor")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when (call.method) {
      "processImage" -> {
        val srcImagePath = call.argument<String>("srcImagePath")
        val outputImageName = call.argument<String>("outputImageName")
        val quality = call.argument<Int>("quality")

        if (srcImagePath != null && outputImageName != null && quality != null) {
          val processedImagePath = processImage(context, srcImagePath, outputImageName, quality)
          result.success(processedImagePath)
        } else {
          result.error("Invalid arguments", "Arguments are missing or invalid", null)
        }
      }
      else -> {
        result.notImplemented()
      }
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  private fun processImage(
    ctx: Context,
    srcImagePath: String,
    outputImageName: String,
    quality: Int
  ): String {

    var resultPath = ""
    try {
      var resizedBitmap: Bitmap? = null
      val decodingOptions = BitmapFactory.Options()
      decodingOptions.inJustDecodeBounds = true

      val srcFile = File(srcImagePath!!)
      val fileBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Files.readAllBytes(srcFile.toPath())
      } else {
        File(srcImagePath).readBytes()
      }
      var imageBitmap = BitmapFactory.decodeByteArray(fileBytes, 0, fileBytes.size, decodingOptions)

      val maxHeightLimit = 1024.0f
      val maxWidthLimit = 912.0f
      var ratio = decodingOptions.outWidth / decodingOptions.outHeight.toFloat()
      val maxRatioLimit = maxWidthLimit / maxHeightLimit

      adjustImageDimensions(decodingOptions.outHeight, decodingOptions.outWidth, maxHeightLimit, maxWidthLimit, ratio, maxRatioLimit)

      decodingOptions.inSampleSize = computeInSampleSize(decodingOptions, decodingOptions.outWidth, decodingOptions.outHeight)
      decodingOptions.inJustDecodeBounds = false
      decodingOptions.inPurgeable = true
      decodingOptions.inInputShareable = true

      decodingOptions.inTempStorage = ByteArray(16 * 1024)
      try {
        imageBitmap = BitmapFactory.decodeFile(srcImagePath, decodingOptions)
      } catch (exception: OutOfMemoryError) {
        exception.printStackTrace()
      }

      try {
        resizedBitmap = Bitmap.createBitmap(decodingOptions.outWidth, decodingOptions.outHeight, Bitmap.Config.ARGB_8888)
      } catch (exception: OutOfMemoryError) {
        exception.printStackTrace()
      }

      val scaleX = decodingOptions.outWidth / decodingOptions.outWidth.toFloat()
      val scaleY = decodingOptions.outHeight / decodingOptions.outHeight.toFloat()
      val centerX = decodingOptions.outWidth / 2.0f
      val centerY = decodingOptions.outHeight / 2.0f

      val transformationMatrix = Matrix()
      transformationMatrix.setScale(scaleX, scaleY, centerX, centerY)

      val drawingCanvas = Canvas(resizedBitmap!!)
      drawingCanvas.setMatrix(transformationMatrix)
      drawingCanvas.drawBitmap(
        imageBitmap,
        centerX - imageBitmap.width / 2,
        centerY - imageBitmap.height / 2,
        Paint(Paint.FILTER_BITMAP_FLAG)
      )

      // Handle rotation
      handleImageRotation(srcImagePath, resizedBitmap)

      val outputStream: FileOutputStream?
      resultPath = outputImageName?.let { generateOutputFilePath(it, ctx) }!!.absolutePath
      try {
        outputStream = FileOutputStream(resultPath)
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
      } catch (e: FileNotFoundException) {
        e.printStackTrace()
      }
    } catch (e: java.lang.Exception) {
      e.printStackTrace()
    }

    return resultPath
  }
}

private fun generateOutputFilePath(imgName: String, ctx: Context): File {
  val file = createImageStorage(ctx, imgName)
  if (file.exists()) file.delete()
  return createImageStorage(ctx, imgName)
}

private fun computeInSampleSize(
  decodingOptions: BitmapFactory.Options,
  desiredWidth: Int,
  desiredHeight: Int
): Int {
  val initialHeight = decodingOptions.outHeight
  val initialWidth = decodingOptions.outWidth
  var samplingSize = 1
  try {
    if (initialHeight > desiredHeight || initialWidth > desiredWidth) {
      val heightFactor = (initialHeight.toFloat() / desiredHeight.toFloat()).roundToInt()
      val widthFactor = (initialWidth.toFloat() / desiredWidth.toFloat()).roundToInt()
      samplingSize = if (heightFactor < widthFactor) heightFactor else widthFactor
    }
    val totalPixelCount = initialWidth * initialHeight.toFloat()
    val maxPixelCount = desiredWidth * desiredHeight * 2.toFloat()
    while (totalPixelCount / (samplingSize * samplingSize) > maxPixelCount) {
      samplingSize++
    }
  } catch (e: java.lang.Exception) {
    e.printStackTrace()
  }
  return samplingSize
}

@Throws(IOException::class)
private fun createImageStorage(ctx: Context, fileName: String): File {
  return File(
    ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
      .toString() + File.separator + fileName + ".png"
  )
}

// New method to handle image dimensions
private fun adjustImageDimensions(
  actualHeight: Int,
  actualWidth: Int,
  maxHeight: Float,
  maxWidth: Float,
  imgRatioParam: Float,
  maxRatio: Float
): Pair<Int, Int> {
  var actualHeightVar = actualHeight
  var actualWidthVar = actualWidth
  var imgRatio = imgRatioParam

  if (actualHeightVar > maxHeight || actualWidthVar > maxWidth) {
    when {
      imgRatio < maxRatio -> {
        imgRatio = maxHeight / actualHeightVar
        actualWidthVar = (imgRatio * actualWidthVar).toInt()
        actualHeightVar = maxHeight.toInt()
      }
      imgRatio > maxRatio -> {
        imgRatio = maxWidth / actualWidthVar
        actualHeightVar = (imgRatio * actualHeightVar).toInt()
        actualWidthVar = maxWidth.toInt()
      }
      else -> {
        actualHeightVar = maxHeight.toInt()
        actualWidthVar = maxWidth.toInt()
      }
    }
  }

  return Pair(actualHeightVar, actualWidthVar)
}


// New method to handle image rotation
private fun handleImageRotation(imagePath: String, resizedBitmap: Bitmap) {
  val exif: ExifInterface
  try {
    exif = ExifInterface(imagePath)
    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)
    Log.d("EXIF", "Exif: $orientation")
    val rotationMatrix = Matrix()
    when (orientation) {
      6 -> {
        rotationMatrix.postRotate(90f)
        Log.d("EXIF", "Exif: $orientation")
      }

      3 -> {
        rotationMatrix.postRotate(180f)
        Log.d("EXIF", "Exif: $orientation")
      }

      8 -> {
        rotationMatrix.postRotate(270f)
        Log.d("EXIF", "Exif: $orientation")
      }
    }
    Bitmap.createBitmap(
      resizedBitmap, 0, 0,
      resizedBitmap.width, resizedBitmap.height, rotationMatrix,
      true
    )
  } catch (e: IOException) {
    e.printStackTrace()
  }
}





