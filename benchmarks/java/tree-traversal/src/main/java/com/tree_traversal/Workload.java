package com.tree_traversal;

import java.util.*;

public class Workload {

    private static class Node {
        int data;
        Node left, right;
    }

    private Node getNode(int data) {
        Node newNode = new Node();
        newNode.data = data;
        newNode.left = newNode.right = null;
        return newNode;
    }


    private Node levelOrder(Node root, int data) {
        if (root == null) {
            root = getNode(data);
            return root;
        }
        if (data <= root.data)
            root.left = levelOrder(root.left, data);
        else
            root.right = levelOrder(root.right, data);
        return root;
    }

    private Node constructBst(int[] arr, int n) {
        if (n == 0)
            return null;
        Node root = null;

        for (int i = 0; i < n; i++)
            root = levelOrder(root, arr[i]);

        return root;
    }

    private int[] generateArray(int number) {
        Random random = new Random();
        int[] res = new int[number];
        for (int i = 0; i < number; i++) {
            res[i] = random.nextInt(Integer.MAX_VALUE);
        }
        return res;
    }

    public int maxDepth(int number) {
        int[] arr = generateArray(number);
        Node root = constructBst(arr, arr.length);
        int level = 0;

        Queue<Node> queue = new LinkedList<>();
        queue.offer(root);
        while (!queue.isEmpty()) {
            level++;
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                Node node = queue.poll();
                if (node.left != null) {
                    queue.offer(node.left);
                }
                if (node.right != null) {
                    queue.offer(node.right);
                }
            }
        }

        return level;
    }
}
