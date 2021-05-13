package com.array_hashing;

import java.util.*;

public class Workload {

    private int[] generateArray(int number) {
        Random random = new Random();
        int[] res = new int[number];
        for (int i = 0; i < number; i++) {
            res[i] = random.nextInt(Environment.NUMBER_RANGE);
        }
        return res;
    }

    public int findLHS(int number) {
        int[] nums = generateArray(number);
        HashMap<Integer, Integer> map = new HashMap<>();
        int res = 0;
        for (int num : nums) {
            map.put(num, map.getOrDefault(num, 0) + 1);
        }
        for (int key : map.keySet()) {
            if (map.containsKey(key + 1))
                res = Math.max(res, map.get(key) + map.get(key + 1));
        }
        return res;
    }
}
