package com.example.user.myapplication.calculateChange

import org.opencv.core.*
import org.opencv.imgproc.Imgproc





class calculateChange(private val img1: Mat, private val img2: Mat) {

    fun affineCalculateChange(q_kp: MatOfPoint2f, t_kp: MatOfPoint2f ) : Mat{
        val calculateChangeImage = Mat()
        val obj_corners = Mat(4, 1, CvType.CV_32FC2)

        obj_corners.put(0, 0, *doubleArrayOf(0.0, 0.0))
        obj_corners.put(1, 0, *doubleArrayOf(img1.cols().toDouble(), 0.0))
        obj_corners.put(2, 0, *doubleArrayOf(img1.cols().toDouble(), img1.rows().toDouble()))
        obj_corners.put(3, 0, *doubleArrayOf(0.0, img1.rows().toDouble()))

        val q_kpList = q_kp.toList()
        val t_kpList = t_kp.toList()
        var q_kpListTo3: MutableList<Point> = mutableListOf()
        var t_kpListTo3: MutableList<Point> = mutableListOf()

        for (i in 0..2) {
            q_kpListTo3.add(i, q_kpList[i])
            t_kpListTo3.add(i, t_kpList[i])
        }

        var q_kpAfter = MatOfPoint2f()
        q_kpAfter.fromList(q_kpListTo3)
        var t_kpAfter = MatOfPoint2f()
        t_kpAfter.fromList(t_kpListTo3)

        val warpMat = Imgproc.getAffineTransform(q_kpAfter, t_kpAfter)
        Imgproc.warpAffine(img1, calculateChangeImage, warpMat, img1.size())

        return calculateChangeImage
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