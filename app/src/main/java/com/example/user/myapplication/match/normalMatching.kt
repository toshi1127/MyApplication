package com.example.user.myapplication.match

import org.opencv.core.*
import org.opencv.features2d.DescriptorMatcher

/**
 * Created by user on 2018/08/08.
 */
class normalMatching(matchAlg: Int, private val distance: Int, private val keypoint1: MatOfKeyPoint, private val keypoint2: MatOfKeyPoint) {
    val matcher = DescriptorMatcher.create(matchAlg)

    fun featurePointMatchs(descriptor1: Mat, descriptor2: Mat): Pair<MutableList<DMatch>, Int> {
        val matches_list: MutableList<DMatch> = mutableListOf()
        val match12 = MatOfDMatch().apply { matcher.match(descriptor1, descriptor2, this) }
        val match21 = MatOfDMatch().apply { matcher.match(descriptor2, descriptor1, this) }
        val size: Int = match12.toArray().size - 1
        val match12_array = match12.toArray()
        val match21_array = match21.toArray()
        var count: Int = 0
        for(i in 0..size) {
            val forward: DMatch =match12_array[i]
            val backward: DMatch = match21_array[forward.trainIdx]
            if(backward.trainIdx == forward.queryIdx) {
                if(backward.distance <= distance){
                    matches_list.add(forward)
                    count++
                }
            }
        }
        return Pair(matches_list, count)
    }

    fun filterMatches(matches_list: MutableList<DMatch>): Pair<MatOfPoint2f, MatOfPoint2f>{
        var pts1: MutableList<Point> = mutableListOf()
        var pts2: MutableList<Point> = mutableListOf()
        for(mat in matches_list) {
            pts1.add(keypoint1.toList().get(mat.queryIdx).pt)
            pts2.add(keypoint2.toList().get(mat.trainIdx).pt)
        }

        var pts1After = MatOfPoint2f()
        pts1After.fromList(pts1)
        var pts2After = MatOfPoint2f()
        pts2After.fromList(pts2)
        return Pair(pts1After, pts2After)
    }
}