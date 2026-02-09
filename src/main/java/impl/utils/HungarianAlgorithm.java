package impl.utils;

import java.util.Arrays;

public class HungarianAlgorithm {

    public static int[] compute(double[][] similarities) {
        int size = Math.max(similarities.length, similarities[0].length) + 1;
        double[][] A = new double[size][size];

        for (int i = 0; i < similarities.length; i++) {
            for (int j = 0; j < similarities[0].length; j++) {
                A[i + 1][j + 1] = -similarities[i][j];
            }
        }

        int[] result = new int[Math.max(similarities.length, similarities[0].length)];
        int[] out = hungarian(A, size - 1, size - 1);
        for (int i = 0; i < result.length; i++) {
            result[i] = out[i + 1] - 1;
        }
        return result;
    }

    // Implementation of the Hungarian Algorithm to search for the optimal matching between
    // the sets of functions in the two programs
    // Based on: https://cp-algorithms.com/graph/hungarian-algorithm.html#implementation-of-the-hungarian-algorithm
    // and: https://web.archive.org/web/20240414033435/http://zafar.cc/2017/7/19/hungarian-algorithm/
    private static int[] hungarian(double[][] A, int n, int m) {

        double[] u = new double[n + 1];
        double[] v = new double[m + 1];
        int[] p = new int[m + 1];
        int[] way = new int[m + 1];

        for (int i = 1; i <= n; ++i) {
            p[0] = i;
            int j0 = 0;

            double[] minv = new double[m + 1];
            Arrays.fill(minv, Double.POSITIVE_INFINITY);
            boolean[] used = new boolean[m + 1];

            do {
                used[j0] = true;
                int i0 = p[j0];
                double delta = Double.POSITIVE_INFINITY;
                int j1 = 0;
                for (int j = 1; j <= m; ++j) {
                    if (!used[j]) {
                        double cur = A[i0][j] - u[i0] - v[j];
                        if (cur < minv[j]) {
                            minv[j] = cur;
                            way[j] = j0;
                        }
                        if (minv[j] < delta) {
                            delta = minv[j];
                            j1 = j;
                        }
                    }
                }
                for (int j = 0; j <= m; ++j) {
                    if (used[j]) {
                        u[p[j]] += delta;
                        v[j] -= delta;
                    } else {
                        minv[j] -= delta;
                    }
                }
                j0 = j1;
            } while (p[j0] != 0);

            do {
                int j1 = way[j0];
                p[j0] = p[j1];
                j0 = j1;
            } while (j0 != 0);
        }

        int[] ans = new int[n + 1];
        for (int j = 1; j <= m; ++j)
            ans[p[j]] = j;
        return ans;
    }
}
