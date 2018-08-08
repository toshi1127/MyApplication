package com.example.user.myapplication.calculateChange

import org.opencv.core.Mat
import org.opencv.core.CvType
import org.opencv.core.Core
import org.opencv.imgproc.Imgproc





class calculateChange(private val img1: Mat, private val img2: Mat) {

    fun affineCalculateChange() {

    }

    fun homographyCalculateChange(H: Mat) : Mat{
        val calculateChangeImage = Mat()

        val obj_corners = Mat(4, 1, CvType.CV_32FC2)
        val scene_corners = Mat(4, 1, CvType.CV_32FC2)

        obj_corners.put(0, 0, *doubleArrayOf(0.0, 0.0))
        obj_corners.put(1, 0, *doubleArrayOf(img1.cols().toDouble(), 0.0))
        obj_corners.put(2, 0, *doubleArrayOf(img1.cols().toDouble(), img1.rows().toDouble()))
        obj_corners.put(3, 0, *doubleArrayOf(0.0, img1.rows().toDouble()))
        Core.perspectiveTransform(obj_corners, scene_corners, H)
        val perspectiveMatrix = Imgproc.getPerspectiveTransform(scene_corners, obj_corners)

        Imgproc.warpPerspective(img1, calculateChangeImage, perspectiveMatrix, img1.size())

        return calculateChangeImage
    }
}