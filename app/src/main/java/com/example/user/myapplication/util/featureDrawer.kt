package com.example.user.myapplication.util

import org.opencv.core.Mat
import org.opencv.core.MatOfKeyPoint
import org.opencv.features2d.AKAZE
import org.opencv.features2d.ORB

/**
 * Created by user on 2018/08/07.
 */
class featureDrawer(detectorName: String, private val img1: Mat, private val img2: Mat) {
    val detector = if (detectorName == "ORB") ORB.create() else AKAZE.create()

    fun featureExtraction () : Pair<MatOfKeyPoint, MatOfKeyPoint>{
        val keypoint1 = MatOfKeyPoint().apply { detector.detect(img1, this) }
        val keypoint2 = MatOfKeyPoint().apply { detector.detect(img2, this) }

        return Pair(keypoint1, keypoint2)
    }

    fun featureDraw (keypoint1: MatOfKeyPoint, keypoint2: MatOfKeyPoint) : Pair<Mat, Mat>{
        val descriptor1 = Mat().apply { detector.compute(img1, keypoint1, this) }
        val descriptor2 = Mat().apply { detector.compute(img2, keypoint2, this) }

        return Pair(descriptor1, descriptor2)
    }

}