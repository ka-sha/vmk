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

    private static final int[][] statStructure = new int[6][64];
    private static final int[][] fourierWithoutDivision = new int[6][64];
    private static final double[][] fourier = new double[6][64];
    private static final int[] bestLinearApproximation = new int[6];
    private static final int[] correlationImmunityOrders = new int[6];
    private static final int[] leastVariableApproximation = new int[6];
    private static final int[][][] linearCharacteristicsWithoutDivision = new int[6][64][64];
    private static final int[][][] diffCharacteristicsWithoutDivision = new int[6][64][64];

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

        fourierWithoutDivision();
        fourier();
        statStructureCoefficients();
        countBestLinearApproximation();
        correlationImmunityOrder();
        countLeastVariableApproximation();
        countLinearCharacteristic();
        countDiffCharacteristic();
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

    private static void fourierWithoutDivision() {
        for (int i = 0; i < 6; i++) {
            int[] tmp = new int[64];
            for (int j = 0; j < 64; j++) {
                tmp[j] = coordinateFunction.get(i)[j] ? 1 : 0;
            }
            int vectorLength = 1;
            while (vectorLength < 64) {
                int[] firstVector = new int[vectorLength];
                int[] secondVector = new int[vectorLength];
                for (int j = 0; j < 64; j += vectorLength * 2) {
                    System.arraycopy(tmp, j, firstVector, 0, vectorLength);
                    System.arraycopy(tmp, j + vectorLength, secondVector, 0, vectorLength);
                    System.arraycopy(IntStream.range(0, vectorLength).map(k -> firstVector[k] + secondVector[k]).toArray(), 0, tmp, j, vectorLength);
                    System.arraycopy(IntStream.range(0, vectorLength).map(k -> firstVector[k] - secondVector[k]).toArray(), 0, tmp, j + vectorLength, vectorLength);
                }
                vectorLength *= 2;
            }
            fourierWithoutDivision[i] = tmp;
        }
    }

    private static void fourier() {
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 64; j++) {
                fourier[i][j] = (double) fourierWithoutDivision[i][j] / 64;
            }
        }
    }

    private static void statStructureCoefficients() {
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 64; j++) {
                if (j == 0) {
                    statStructure[i][j] = 32 - fourierWithoutDivision[i][j];
                } else {
                    statStructure[i][j] = -fourierWithoutDivision[i][j];
                }
            }
        }
    }

    private static void countBestLinearApproximation() {
        for (int i = 0; i < 6; i++) {
            int approximation = 0;
            int max = 0;
            for (int j = 0; j < 64; j++) {
                if (Math.abs(statStructure[i][j]) > Math.abs(max)) {
                    approximation = j;
                    max = statStructure[i][j];
                }
            }
            bestLinearApproximation[i] = max >= 0 ? approximation : -approximation;
        }
    }

    private static void correlationImmunityOrder() {
        for (int i = 0; i < 6; i++) {
            int order = 1;
            boolean orderFound = false;
            while (order <= 6) {
                for (int j = 0; j < 64; j++) {
                    if (Integer.toBinaryString(j).chars().filter(c -> c == '1').count() == order) {
                        if (fourier[i][j] != 0) {
                            orderFound = true;
                            break;
                        }
                    }
                }
                if (orderFound) {
                    break;
                }
                order++;
            }
            correlationImmunityOrders[i] = order - 1;
        }
    }

    private static void countLeastVariableApproximation() {
        for (int i = 0; i < 6; i++) {
            int max = 0;
            int approximation = 0;
            for (int j = 0; j < 64; j++) {
                if (Integer.toBinaryString(j).chars().filter(c -> c == '1').count() == correlationImmunityOrders[i] + 1) {
                    if (Math.abs(statStructure[i][j]) > Math.abs(max)) {
                        max = statStructure[i][j];
                        approximation = j;
                    }
                }
            }
            leastVariableApproximation[i] = max >= 0 ? approximation : -approximation;
        }
    }

    private static void countLinearCharacteristic() {
        for (int i = 0; i < 6; i++) {
            for (int a = 0; a < 64; a++) {
                for (int b = 0; b < 64; b++) {
                    for (int x = 0; x < 64; x++) {
                        if ((scalar(x, a) ^ scalar(sBox[x], b)) == 0) {
                            linearCharacteristicsWithoutDivision[i][a][b]++;
                        }
                    }
                }
            }
        }
    }

    private static int scalar(int v1, int v2) {
        return Integer.bitCount(v1 & v2) % 2;
    }

    private static void countDiffCharacteristic() {
        for (int i = 0; i < 6; i++) {
            for (int a = 0; a < 64; a++) {
                for (int b = 0; b < 64; b++) {
                    for (int x = 0; x < 64; x++) {
                        if ((sBox[x] ^ sBox[x ^ a]) == b) {
                            diffCharacteristicsWithoutDivision[i][a][b]++;
                        }
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            result.append("f").append(i).append(": ");
            for (int j = 0; j < 64; j++) {
                result.append(coordinateFunction.get(i)[j] ? "1" : "0");
            }
            result.append("\n");
        }

        result.append("\n");

        for (int i = 0; i < 6; i++) {
            result.append("f").append(i).append(" weight: ").append(functionWeight[i]).append("\n");
        }

        result.append("\n");

        result.append("Zhegalkin Polynomials:\n");
        for (int i = 0; i < 6; i++) {
            result.append("f").append(i).append(": ").append(zhegalkinPolynomialToString(ZhegalkinPolynomial.get(i))).append("\n");
        }

        result.append("\n");

        result.append("Dummy variables:\n");
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            StringBuilder a = new StringBuilder();
            for (int j = 0; j < 6; j++) {
                if (dummyVariables.get(i)[j]) {
                    a.append("x").append(j).append(", ");
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
            result.append("Ban for f").append(i).append(": ").append(path.substring(1)).append("\n");
            path = "";
            prevNodes = new ArrayList<>();
            prohibitionFound = false;
        }

        findProhibitionOfSBox(new HashSet<>(Arrays.stream(IntStream.range(0, 64).toArray())
                        .boxed()
                        .collect(Collectors.toList())),
                7, path);
        result.append("Ban of S-box: ").append(path.substring(1)).append("\n");

        for (int i = 0; i < 6; i++) {
            result.append("Fourier for f").append(i).append(": ").append(Arrays.toString(fourierWithoutDivision[i])).append("\n");
        }

        for (int i = 0; i < 6; i++) {
            result.append("Static struct coefficient f").append(i).append(": ").append(Arrays.toString(statStructure[i])).append("\n");
        }

        for (int i = 0; i < 6; i++) {
            result.append("Best linear approximation f").append(i).append(": ").append(bestLinearApproximation[i]).append("\n");
        }

        for (int i = 0; i < 6; i++) {
            result.append("Order of correlation immunity f").append(i).append(": ").append(correlationImmunityOrders[i]).append("\n");
        }

        for (int i = 0; i < 6; i++) {
            result.append("Best least approximation f").append(i).append(": ").append(leastVariableApproximation[i]).append("\n");
        }

        for (int i = 0; i < 6; i++) {
            result.append("Linear characteristics f").append(i).append(":\n");
            for (int j = 0; j < 64; j++) {
                result.append(Arrays.toString(linearCharacteristicsWithoutDivision[i][j])).append("\n");
            }
        }

        for (int i = 0; i < 6; i++) {
            result.append("Difference characteristics f").append(i).append(":\n");
            for (int j = 0; j < 64; j++) {
                result.append(Arrays.toString(diffCharacteristicsWithoutDivision[i][j])).append("\n");
            }
        }

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
