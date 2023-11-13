package uni.parallel.onlinemandel.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RestController
public class MandelbrotController {

    @GetMapping("/mandel")
    public String computeMandelbrotSet(@RequestParam("zoom") double zoom,
    @RequestParam("threads") int numThreads) {
        int width = 800;
        int height = 600;
        int maxIterations = 1000;

        StringBuilder ppm = new StringBuilder();
        ppm.append("P3\n");
        ppm.append(width).append(" ").append(height).append("\n");
        ppm.append("255\n");

        int[] set = computeMandelbrotSet(width, height, zoom, maxIterations, numThreads);

        for (int i = 0; i < set.length; i++) {
            ppm.append(set[i] + " ");
        }

        return ppm.toString();
    }

    private int[] computeMandelbrotSet(int width, int height, double zoom, int maxIterations, int numThreads) {
        int[] ppmData = new int[width * height * 3];

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<Void>> futures = new ArrayList<>();

        for (int y = 0; y < height; y++) {
            final int row = y;
            futures.add(executor.submit(() -> {
                for (int x = 0; x < width; x++) {
                    double zx = 0;
                    double zy = 0;
                    double cx = (x - width / 2.0) * zoom / width;
                    double cy = (row - height / 2.0) * zoom / height;
                    int iterations = 0;

                    while (zx * zx + zy * zy <= 4 && iterations < maxIterations) {
                        double xtemp = zx * zx - zy * zy + cx;
                        zy = 2 * zx * zy + cy;
                        zx = xtemp;
                        iterations++;
                    }

                    int color = iterations % 256;
                    ppmData[(row * width + x) * 3] = colorPalette.get(color % (colorPalette.size() - 1))[0];
                    ppmData[(row * width + x) * 3 + 1] = colorPalette.get(color % (colorPalette.size() - 1))[1];
                    ppmData[(row * width + x) * 3 + 2] =  colorPalette.get(color % (colorPalette.size() - 1))[2];
                }
                return null;
            }));
        }

        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                // Handle exception
            }
        }

        executor.shutdown();

        return ppmData;
    }

    public final static List<int[]> colorPalette = getPalette();

    private static int lagrange(int[] dot1, int[] dot2, int x) {
        return ((dot1[1] * (x - dot2[0])) / (dot1[0] - dot2[0])) + ((dot2[1] * (x - dot1[0])) / (dot2[0] - dot1[0]));
    }

    private static List<int[]> getPalette() {
        int size = 250;
        int range = size / 6;
        List<int[]> colors = new ArrayList<>();
        int lagrangeColor;
        for (int k = 0; k < size; k++) {
            if (k <= range) { //red to yellow
                lagrangeColor = lagrange(new int[] {0, 0}, new int[] {range, 255}, k);
                colors.add(new int[] {255, lagrangeColor, 0});
            } else if (k <= range * 2) { //yellow to green
                lagrangeColor = lagrange(new int[] {range + 1, 255}, new int[] {range * 2, 0}, k);
                colors.add(new int[] {lagrangeColor, 255, 0});
            } else if (k <= range * 3) {//green to cyan
                lagrangeColor = lagrange(new int[] {range * 2 + 1, 0}, new int[] {range * 3, 255}, k);
                colors.add(new int[] {0, 255, lagrangeColor});
            } else if (k <= range * 4) {//cyan to blue
                lagrangeColor = lagrange(new int[] {range * 3 + 1, 255}, new int[] {range * 4, 0}, k);
                colors.add(new int[] {0, lagrangeColor, 255});
            } else if (k <= range * 5) {//blue to purple
                lagrangeColor = lagrange(new int[] {range * 4 + 1, 0}, new int[] {range * 5, 255}, k);
                colors.add(new int[] {lagrangeColor, 0, 255});
            } else {//purple to red
                lagrangeColor = lagrange(new int[] {range * 5 + 1, 255}, new int[] {size - 1, 0}, k);
                colors.add(new int[] {255, 0, lagrangeColor});
            }
        }
        return colors;
    }
}

