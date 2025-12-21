// lire et écrire des images
import javax.imageio.ImageIO;
//modifier des pixels
import java.awt.image.BufferedImage;
//creer des fichiers ou repertoire
import java.io.File;
//gere les erreurs lié aux fichiers
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class Brouillimg {

    public static void main(String[] args) throws IOException {

        if (args.length < 3) {

            System.err.println(
                    "Usage: java Brouillimg <image_claire> <clé (optionel si cassage)> <scramble(0), unscramble(1), cassage euclidien(2), cassage Pearson(3), ou cassage nul(4)> [image_sortie]");

            System.exit(1);

        }

        String inPath = args[0];

        String outPath = (args.length >= 4) ? args[3] : "out.png";

        int key = Integer.parseInt(args[1]) & 0x7FFF;

        int action = Integer.parseInt(args[2]);

        BufferedImage inputImage = ImageIO.read(new File(inPath));

        if (inputImage == null) {

            throw new IOException("Format d'image non reconnu: " + inPath);

        }

        final int height = inputImage.getHeight();

        final int width = inputImage.getWidth();

        System.out.println("Dimensions de l'image : " + width + "x" + height);

        int[][] inputImageGL = rgb2gl(inputImage);

        int[] perm = generatePermutation(height, key);

        BufferedImage scrambledImage;

        switch (action) {
            case 0:
                scrambledImage = scrambleLines(inputImage, perm);
                break;

            case 1:
                scrambledImage = unscrambleLines(inputImage, perm);
                break;

            case 2:
                perm = generatePermutation(height, breakkeyEuclidean(inputImageGL));
                scrambledImage = unscrambleLines(inputImage, perm);
                break;
            case 3:
                perm = generatePermutation(height, breakkeyPearson(inputImageGL));
                scrambledImage = unscrambleLines(inputImage, perm);
                break;
            case 4:
                perm = generatePermutation(height, breakkeyDifference(inputImageGL));
                scrambledImage = unscrambleLines(inputImage, perm);
                break;
            default:
                System.out.println(
                        "entrer une valeur entre 0 1 2 et 3 pour <scramble, unscramble, cassage euclidien et cassage pearson>");
                System.exit(1);
                scrambledImage = unscrambleLines(inputImage, perm);
        }

        ImageIO.write(scrambledImage, "png", new File(outPath));

        System.out.println("Image écrite: " + outPath);

    }

    //calcul de différence avec pixel du dessous et de droite
    public static int differencebasdroite(int[][] inputGL){
        int differencetotal = 0;
        int lenght = inputGL.length;
        for(int i = 0; i< lenght - 1; i++){
            for (int j= 0; j<inputGL[0].length-1;j++){
                differencetotal += Math.abs(inputGL[i][j] - inputGL[i+1][j]);
                differencetotal += Math.abs(inputGL[i][j] - inputGL[i][j+1]);
            }
        }
        return differencetotal;
    }

    //comparaison des scores de differencebasdroite
    public static int testcassage(int[][] inputGL){
        AtomicInteger bestKey = new AtomicInteger(0);
        AtomicInteger bestScore = new AtomicInteger(Integer.MAX_VALUE);

        IntStream.range(0, 32768)
                .parallel()
                .forEach(key -> {
                    int[] perm = generatePermutation(inputGL.length, key);
                    int[][] candidate = unscrambleLinesGL(inputGL, perm);
                    int score = differencebasdroite(candidate);

                    synchronized(bestScore) {
                        if (score < bestScore.get()) {
                            bestScore.set(score);
                            bestKey.set(key);
                        }
                    }
                });

        return bestKey.get();
    }

    public static float corelationPearson(int[] premiereLigne, int[] deuxiemeLigne) {

        long totalPremiere = 0;
        long totalSeconde = 0;
        for (int i = 0; i < premiereLigne.length; ++i) {
            totalPremiere += premiereLigne[i];
            totalSeconde += deuxiemeLigne[i];
        }
        float moyPremiere = (float) totalPremiere / premiereLigne.length;
        float moySeconde = (float) totalSeconde / premiereLigne.length;

        float covarianceXY = 0;
        float varianceX = 0;
        float varianceY = 0;

        for (int i = 0; i < premiereLigne.length; ++i) {
            float diffX = premiereLigne[i] - moyPremiere;
            float diffY = deuxiemeLigne[i] - moySeconde;

            covarianceXY += diffX * diffY;
            varianceX += diffX * diffX;
            varianceY += diffY * diffY;
        }

        if (varianceX == 0 || varianceY == 0) {
            return 0;
        }

        return covarianceXY / (float) Math.sqrt(varianceX * varianceY);
    }

    public static float scorePearson(int[][] imageGL) {
        float scoreTotal = 0;
        for (int i = 0; i < imageGL.length - 1; ++i) {
            scoreTotal += corelationPearson(imageGL[i], imageGL[i + 1]);
        }
        return scoreTotal;
    }

    public static double euclideanDistance(int[] xGL, int[] yGL) {
        int width = xGL.length;
        double disteuclid = 0;
        for (int i = 0; i < width; i++) {
            int diff = xGL[i] - yGL[i];
            disteuclid += diff * diff;
        }
        return Math.sqrt(disteuclid);

    }

    public static double scoreEuclidean(int[][] inputGL) {
        double scoretotal = 0;
        int height = inputGL.length;
        for (int i = 0; i < height - 1; i++) {
            scoretotal += euclideanDistance(inputGL[i], inputGL[i + 1]);
        }
        return scoretotal;
    }

    public static int breakkeyEuclidean(int[][] inputGL) {
        int bestKey = 0;
        double bestScore = Double.MAX_VALUE;
        for (int key = 0; key < 32768; key++) {
            int[] perm = generatePermutation(inputGL.length, key);
            int[][] candidate = unscrambleLinesGL(inputGL, perm);
            double score = scoreEuclidean(candidate);

            if (score < bestScore) {
                bestScore = score;
                bestKey = key;
            }
        }
        return bestKey;
    }

    public static int breakkeyPearson(int[][] inputGL) {
        int bestKey = 0;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int key = 0; key < 32768; key++) {
            int[] perm = generatePermutation(inputGL.length, key);
            int[][] candidate = unscrambleLinesGL(inputGL, perm);
            float score = scorePearson(candidate);

            if (score > bestScore) {
                bestScore = score;
                bestKey = key;
            }
        }
        return bestKey;
    }

    public static int breakkeyDifference(int[][] inputGL) {
        int bestKey = 0;
        int bestScore = Integer.MAX_VALUE;
        for (int key = 0; key < 32768; key++) {
            int[] perm = generatePermutation(inputGL.length, key);
            int[][] candidate = unscrambleLinesGL(inputGL, perm);
            int score = differencebasdroite(candidate);

            if (score < bestScore) {
                bestScore = score;
                bestKey = key;
            }
        }
        return bestKey;
    }

    public static int[][] unscrambleLinesGL(int[][] inputGL, int[] perm) {

        int height = inputGL.length;
        int width = inputGL[0].length;

        if (perm.length != height)
            throw new IllegalArgumentException("Taille image <> permutation");

        int[][] out = new int[height][width];

        for (int y = 0; y < height; y++) {
            int autreY = perm[y];
            out[y] = inputGL[autreY].clone();
        }

        return out;
    }

    public static int[][] rgb2gl(BufferedImage inputRGB) {

        final int height = inputRGB.getHeight();

        final int width = inputRGB.getWidth();

        int[][] outGL = new int[height][width];

        for (int y = 0; y < height; y++) {

            for (int x = 0; x < width; x++) {

                int argb = inputRGB.getRGB(x, y);

                int r = (argb >> 16) & 0xFF;

                int g = (argb >> 8) & 0xFF;

                int b = argb & 0xFF;

                int gray = (r * 299 + g * 587 + b * 114) / 1000;

                outGL[y][x] = gray;

            }

        }

        return outGL;

    }

    public static int[] generatePermutation(int size, int key) {

        int[] scrambleTable = new int[size];

        for (int i = 0; i < size; i++)
            scrambleTable[i] = scrambledId(i, size, key);

        return scrambleTable;

    }

    public static BufferedImage scrambleLines(BufferedImage inputImg, int[] perm) {

        int width = inputImg.getWidth();

        int height = inputImg.getHeight();

        if (perm.length != height)
            throw new IllegalArgumentException("Taille d'image <> taille permutation");

        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            int autreY = perm[y];
            for (int x = 0; x < width; x++) {
                int couleur = inputImg.getRGB(x, y);
                out.setRGB(x, autreY, couleur);
            }
        }

        return out;

    }

    public static BufferedImage unscrambleLines(BufferedImage inputImg, int[] perm) {

        int width = inputImg.getWidth();
        int height = inputImg.getHeight();

        if (perm.length != height)
            throw new IllegalArgumentException("Taille d'image <> taille permutation");

        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            int autreY = perm[y];
            for (int x = 0; x < width; x++) {
                int couleur = inputImg.getRGB(x, autreY);
                out.setRGB(x, y, couleur);
            }
        }

        return out;
    }

    public static int scrambledId(int id, int size, int key) {
        int s = key & 0x7F;
        int r = (key >> 7) & 0xFF;

        return (r + (2 * s + 1) * id) % size;
    }

}