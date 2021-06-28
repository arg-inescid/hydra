package com.array_sorting;

import java.util.*;

public class Workload {

    private int[] generateArray(int number) {
        Random random = new Random();
        int[] res = new int[number];
        for (int i = 0; i < number; i++) {
            res[i] = random.nextInt(Integer.MAX_VALUE);
        }
        return res;
    }

    private boolean isSorted(int[] result) {
        for (int i = 0; i < result.length - 1; i++) {
            if (result[i] > result[i + 1]) {
                return false;
            }
        }
        return true;
    }

    public boolean intersect(int number) {
        int[] nums1 = generateArray(number);
        int[] nums2 = generateArray(number);

        Arrays.sort(nums1);
        Arrays.sort(nums2);
        int p1 = 0, p2 = 0, count = 0;
        int[] tmp = new int[Math.max(nums1.length, nums2.length)], res;

        while (p1 < nums1.length && p2 < nums2.length) {
            if (nums1[p1] == nums2[p2]) {
                tmp[count++] = nums1[p1];
                p1++;
                p2++;
            } else if (nums1[p1] < nums2[p2]) {
                p1++;
            } else {
                p2++;
            }
        }

        res = new int[count];
        System.arraycopy(tmp, 0, res, 0, count);

        return isSorted(res);
    }
}
