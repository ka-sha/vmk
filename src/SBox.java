import java.util.*;

public class SBox {
    private static final int[] sBox =
            {52, 49, 27, 6, 20, 40, 28, 51, 5,
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

    static {
        for (int i = 0; i < 6; i++) {
            coordinateFunction.add(new boolean[64]);
            dummyVariables.add(new boolean[6]);
            Arrays.fill(dummyVariables.get(i), true);
        }

        for (int i = 0; i < 64; i++) {
            char[] res = Integer.toBinaryString(sBox[i]).toCharArray();
            for (int j = res.length - 1, k = 5; j >= 0; j--, k--)
                if (res[j] == '1')
                    coordinateFunction.get(k)[i] = true;
        }

        for (int i = 0; i < 6; i++)
            for (int j = 0; j < coordinateFunction.get(i).length; j++)
                if (coordinateFunction.get(i)[j])
                    functionWeight[i]++;

        for (int i = 0; i < 6; i++)
            ZhegalkinPolynomial.add(triangleMethod(coordinateFunction.get(i)));

        for (int i = 0; i < 6; i++) {
            boolean[] f = ZhegalkinPolynomial.get(i);
            for (int j = 0; j < f.length; j++) {
                if (f[j]) {
                    char[] binary = Integer.toBinaryString(j).toCharArray();
                    for (int k = 0; k < binary.length; k++)
                        if (binary[k] == '1')
                            dummyVariables.get(i)[k] = false;
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
            for (int i = 1; i < curResult.length; i++)
                curResult[i - 1] = curResult[i - 1] ^ curResult[i];
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
            for (int j = 0; j < 6; j++)
                result.append((coordinateFunction.get(j)[i] ? "1" : "0")).append("  ");
            result.append("\n");
        }

        result.append("\n");

        for (int i = 0; i < 6; i++)
            result.append("f").append(i + 1).append(" weight: ").append(functionWeight[i]).append("\n");

        result.append("\n");

        result.append("Zhegalkin Polynomials:\n");
        for (int i = 0; i < 6; i++)
            result.append("f").append(i + 1).append(": ").append(zhegalkinPolynomialToString(ZhegalkinPolynomial.get(i))).append("\n");

        result.append("\n");

        result.append("Dummy variables:\n");
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            StringBuilder a = new StringBuilder();
            for (int j = 0; j < 6; j++) {
                if (dummyVariables.get(i)[j])
                    a.append("x").append(j + 1).append(", ");
            }
            if (a.length() != 0)
                res.append("f").append(i + 1).append(": ").append(a).deleteCharAt(result.length() - 2).append("\n");
        }
        if (res.length() == 0)
            result.append("Does not exist.");
        else
            result.append(res);

        return result.toString();
    }

    private String zhegalkinPolynomialToString(boolean[] polynomial) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < polynomial.length; i++) {
            if(polynomial[i]) {
                char[] ch = String.format("%6s", Integer.toBinaryString(i)).toCharArray();
                StringBuilder term = new StringBuilder();
                for(int j = 0; j < ch.length; j++) {
                    if(ch[j] == '1') {
                        term.append("x").append(j + 1);
                    }
                }
                if (term.length() == 0)
                    term.append("1");
                res.append(term).append(" + ");
            }
        }
        return res.substring(0, res.length() - 3);
    }

    public static void main(String[] args) {
        System.out.println(new SBox().toString());
    }
}
