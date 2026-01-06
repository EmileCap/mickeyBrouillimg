import javax.imageio.ImageIO;
//modifier des pixels
import java.awt.image.BufferedImage;
//creer des fichiers ou repertoire
import java.io.File;
//gere les erreurs lié aux fichiers
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;


import java.io.IOException;          // pb.start()
import java.lang.ProcessBuilder;     // créer un processus

import java.util.Random;


public class profiler {

    Random rand = new Random();

    public static void main(String[] args) throws IOException, InterruptedException {

        if (args.length < 4) {

            System.err.println("Usage: java Brouillimg <image_claire> <clé (optionel si cassage)> <scramble(0), unscramble(1), cassage euclidien(2), cassage Pearson(3), ou cassage nul(4)> <choix de l'annalyse> [image_sortie]");

            System.exit(1);

        }

        int[][] scoreCase2 = new int[2][1];

        switch (args[3]) {
            case "2" :
                scoreCase2 = analyseResulta();
                showTab(scoreCase2);
                break;
            default:
                System.out.println("valeur incorrecte pour l'argument 3");
                System.exit(1);
        }

    }


    public static int[][] analyseResulta() throws IOException, InterruptedException {
        Random rand = new Random();
        int n = 488;
        String nomImage = ".png";
        int[][] result = new int[9][3];
        int nb;
        System.out.println("ICI" );

        for (int j = 0; j < 5; j++) {
            System.out.println("LA" );

            nb = j + 1;
            nomImage = nb + ".png";

            ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    "Brouillimg",
                    nomImage,
                    String.valueOf(n),
                    "0"
            );

            Process p = pb.start();
            p.waitFor();

            for (int k = 0; k < 3; k++) {
                System.out.println("par ici" );

                pb = new ProcessBuilder(
                        "java",
                        "Brouillimg",
                        "out.png",
                        String.valueOf(n),
                        String.valueOf(k+2)
                );

                p = pb.start();
                p.waitFor();

                System.out.println("la bas" );


                BufferedImage imgRef = ImageIO.read(new File(nomImage));
                BufferedImage imgOut = ImageIO.read(new File("out.png"));

                int score = compareImages(imgRef, imgOut);
                result[j][k] = score;
                System.out.println("Erreur image = " + score + " methode = " + k);
            }
        }

        return result;
    }


    public static int compareImages(BufferedImage imgBase, BufferedImage imgDecr) throws IOException {
        System.out.println("par la" );

        int width = imgBase.getWidth();
        int height = imgBase.getHeight();
        int score = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = imgBase.getRGB(x, y);
                int argb2 = imgDecr.getRGB(x, y);
                if (argb != argb2) {
                    score += 1;
                }
            }
        }
        score /= width;
        return score;
    }


    public static void showTab(int[][] scoreCase2) {
        int rows = scoreCase2.length;
        int cols = scoreCase2[0].length;

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                System.out.print(scoreCase2[y][x] + "\t");
            }
            System.out.println();
        }
    }

}