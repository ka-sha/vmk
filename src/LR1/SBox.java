package LR1;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SBox {
    private static final int[] sBox = {
            52, 49, 27, 6, 20, 40, 28, 51, 5,
            45, 24, 11, 22, 31, 36, 15, 46,
            41, 23, 37, 35, 25, 62, 50, 34,
            61, 42, 38, 39, 4, 59, 60, 58,
            44, 55, 16, 21, 53, 12, 3, 43,
            26, 48, 18, 57, 33, 13, 30, 10,
            1, 56, 47, 8, 0, 7, 2, 54,
            9, 63, 32, 19, 29, 17, 14};

    private static final List<boolean[]> coordinateFunction = new ArrayList<>();
    private static final int[] functionWeight = new int[6];
    private static final List<boolean[]> ZhegalkinPolynomial = new ArrayList<>();
    private static final List<boolean[]> dummyVariables = new ArrayList<>();

    private static List<Set<Integer>> prevNodes = new ArrayList<>();
    private static String path = "";
    private static boolean prohibitionFound = false;

    static {
        for (int i = 0; i < 6; i++) {
            coordinateFunction.add(new boolean[64]);
            dummyVariables.add(new boolean[6]);
            Arrays.fill(dummyVariables.get(i), true);
        }

        for (int i = 0; i < 64; i++) {
            char[] res = Integer.toBinaryString(sBox[i]).toCharArray();
            for (int j = res.length - 1, k = 5; j >= 0; j--, k--) {
                if (res[j] == '1') {
                    coordinateFunction.get(k)[i] = true;
                }
            }
        }

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < coordinateFunction.get(i).length; j++) {
                if (coordinateFunction.get(i)[j]) {
                    functionWeight[i]++;
                }
            }
        }

        for (int i = 0; i < 6; i++) {
            ZhegalkinPolynomial.add(triangleMethod(coordinateFunction.get(i)));
        }

        for (int i = 0; i < 6; i++) {
            boolean[] f = ZhegalkinPolynomial.get(i);
            for (int j = 0; j < f.length; j++) {
                if (f[j]) {
                    char[] binary = Integer.toBinaryString(j).toCharArray();
                    for (int k = 0; k < binary.length; k++) {
                        if (binary[k] == '1') {
                            dummyVariables.get(i)[k] = false;
                        }
                    }
                }
            }
        }
    }

    private static boolean[] triangleMethod(boolean[] line) {
        boolean[] result = new boolean[line.length];
        boolean[] curResult = new boolean[line.length];
        System.arraycopy(line, 0, curResult, 0, line.length);
        int resultIndex = 0;

        while (curResult.length > 0) {
            result[resultIndex++] = curResult[0];
            for (int i = 1; i < curResult.length; i++) {
                curResult[i - 1] = curResult[i - 1] ^ curResult[i];
            }
            boolean[] tmp = new boolean[curResult.length - 1];
            System.arraycopy(curResult, 0, tmp, 0, tmp.length);
            curResult = new boolean[tmp.length];
            System.arraycopy(tmp, 0, curResult, 0, curResult.length);
        }

        return result;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("    f1 f2 f3 f4 f5 f6\n");

        for (int i = 0; i < 64; i++) {
            result.append(String.format("%2d: ", sBox[i]));
            for (int j = 0; j < 6; j++) {
                result.append((coordinateFunction.get(j)[i] ? "1" : "0")).append("  ");
            }
            result.append("\n");
        }

        result.append("\n");

        for (int i = 0; i < 6; i++) {
            result.append("f").append(i + 1).append(" weight: ").append(functionWeight[i]).append("\n");
        }

        result.append("\n");

        result.append("Zhegalkin Polynomials:\n");
        for (int i = 0; i < 6; i++) {
            result.append("f").append(i + 1).append(": ").append(zhegalkinPolynomialToString(ZhegalkinPolynomial.get(i))).append("\n");
        }

        result.append("\n");

        result.append("Dummy variables:\n");
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            StringBuilder a = new StringBuilder();
            for (int j = 0; j < 6; j++) {
                if (dummyVariables.get(i)[j]) {
                    a.append("x").append(j + 1).append(", ");
                }
            }
            if (a.length() != 0) {
                res.append("f").append(i + 1).append(": ").append(a).deleteCharAt(result.length() - 2).append("\n");
            }
        }
        if (res.length() == 0) {
            result.append("Does not exist.\n");
        } else {
            result.append(res);
        }
        for (int i = 0; i < 6; i++) {
            findProhibitionOfVectorFunction(new HashSet<>(Arrays.stream(IntStream.range(0, 64).toArray())
                            .boxed()
                            .collect(Collectors.toList())),
                    coordinateFunction.get(i), 7, path);
            result.append("Запрет для f").append(i).append(": ").append(path.substring(1)).append("\n");
            path = "";
            prevNodes = new ArrayList<>();
            prohibitionFound = false;
        }

        findProhibitionOfSBox(new HashSet<>(Arrays.stream(IntStream.range(0, 64).toArray())
                .boxed()
                .collect(Collectors.toList())),
                7, path);
        result.append("Запрет S-box'а: ").append(path.substring(1)).append("\n");

        return result.toString();
    }

    private static String zhegalkinPolynomialToString(boolean[] polynomial) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < polynomial.length; i++) {
            if (polynomial[i]) {
                char[] ch = String.format("%6s", Integer.toBinaryString(i)).toCharArray();
                StringBuilder term = new StringBuilder();
                for (int j = 0; j < ch.length; j++) {
                    if (ch[j] == '1') {
                        term.append("x").append(j + 1);
                    }
                }
                if (term.length() == 0) {
                    term.append("1");
                }
                res.append(term).append(" + ");
            }
        }
        return res.substring(0, res.length() - 3);
    }

    private void findProhibitionOfVectorFunction(Set<Integer> vectors, boolean[] function, int edge, String path) {
        Node node = new Node(vectors, edge, new StringBuilder(path));
        if (prohibitionFound || prevNodes.contains(node.vectors)) {
            return;
        }
        if (node.isEmpty()) {
            prohibitionFound = true;
            SBox.path = node.prohibition.toString();
            return;
        }
        prevNodes.add(node.vectors);
        node.vectors = expandNode(node.vectors);
        Set<Integer> leftNode = new HashSet<>();
        Set<Integer> rightNode = new HashSet<>();
        for (int vector : node.vectors) {
            if (function[vector]) {
                rightNode.add(vector);
            } else {
                leftNode.add(vector);
            }
        }
        if (rightNode.size() >= leftNode.size()) {
            findProhibitionOfVectorFunction(leftNode, function, 0, node.prohibition.toString());
            findProhibitionOfVectorFunction(rightNode, function, 0, node.prohibition.toString());
        } else {
            findProhibitionOfVectorFunction(rightNode, function, 1, node.prohibition.toString());
            findProhibitionOfVectorFunction(leftNode, function, 1, node.prohibition.toString());
        }
    }

    private static void findProhibitionOfSBox(Set<Integer> vectors, int edge, String path) {
        Node node = new Node(vectors, edge, new StringBuilder(path));
        if (prohibitionFound || prevNodes.contains(node.vectors)) {
            return;
        }
        if (node.isEmpty()) {
            prohibitionFound = true;
            SBox.path = node.prohibition.toString();
            return;
        }
        prevNodes.add(node.vectors);
        node.vectors = expandNode(node.vectors);
        Set<Integer>[] children = new HashSet[64];
        for (int i = 0; i < 64; i++) {
            children[i] = new HashSet<>();
        }
        for (int vector : node.vectors) {
            children[sBox[vector]].add(vector);
        }
        for (int i = 0; i < 64; i++) {
            findProhibitionOfSBox(children[i], i, node.prohibition.toString());
        }
    }

    private static Set<Integer> expandNode(Set<Integer> node) {
        Set<Integer> result = new HashSet<>();
        for (int vector : node) {
            result.add((0x1f & vector) << 1);
            result.add(((0x1f & vector) << 1) + 1);
        }
        return result;
    }

    private static class Node {
        private Set<Integer> vectors;
        private final StringBuilder prohibition;

        public Node(Set<Integer> vectors, int edge, StringBuilder parentPath) {
            this.vectors = vectors;
            this.prohibition = parentPath.append(edge);
        }

        public boolean isEmpty() {
            return vectors.isEmpty();
        }
    }

    public static void main(String[] args) {
        System.out.println(new SBox());
    }
}
