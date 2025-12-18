// lire et écrire des images
import javax.imageio.ImageIO;
//modifier des pixels
import java.awt.image.BufferedImage;
//creer des fichiers ou repertoire
import java.io.File;
//gere les erreurs lié aux fichiers
import java.io.IOException;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.concurrent.atomic.AtomicLong;

import java.util.stream.IntStream;


public class Brouillimg {


    public static void main(String[] args) throws IOException {

        if (args.length < 3) {

            System.err.println("Usage: java Brouillimg <image_claire> <clé> <scramble, unscramble ou cassage> [image_sortie]");

            System.exit(1);

        }

        String inPath = args[0];

        String outPath = (args.length >= 4) ? args[3] : "out.png";

        // Masque 0x7FFF pour garantir que la clé ne dépasse pas les 15 bits

        int key = Integer.parseInt(args[1]) & 0x7FFF ; //clé de chiffrement

        int action = Integer.parseInt(args[2]); // 0 = brouiller, 1 = débrouiller

        BufferedImage inputImage = ImageIO.read(new File(inPath)); //image brouillé

        if (inputImage == null) { // si erreur de fichier

            throw new IOException("Format d’image non reconnu: " + inPath);

        }


        final int height = inputImage.getHeight(); //hauteur de l'image brouillé pour l'image nette

        final int width = inputImage.getWidth(); //largeur de l'image brouillé pour l'image nette

        System.out.println("Dimensions de l'image : " + width + "x" + height);


        // Pré‑calcul des lignes en niveaux de gris pour accélérer le calcul du critère

        int[][] inputImageGL = rgb2gl(inputImage); //image en niveau d gris

        int[] perm = generatePermutation(height, key); //permutation de hauteur par la clé

        BufferedImage scrambledImage;


        if (action == 0) {
            scrambledImage = scrambleLines(inputImage, perm);
        } else if (action == 1){
            scrambledImage = unscrambleLines(inputImage, perm);
        }
        else{
            perm = generatePermutation(height, breakkey(inputImageGL));
            scrambledImage = unscrambleLines(inputImage ,perm);
        }


        ImageIO.write(scrambledImage, "png", new File(outPath));

        System.out.println("Image écrite: " + outPath);



    }
    public static double euclideanDistance(int[] xGL, int[] yGL){
        int width = xGL.length;
        double disteuclid = 0;
        for(int i = 0; i< width ; i++){
            int diff = xGL[i] - yGL[i];
            disteuclid += diff * diff;
        }
        return Math.sqrt(disteuclid);


    }
    public static double scoreEuclidean(int[][] inputGL){
        double scoretotal = 0;
        int height = inputGL.length;
        for(int i = 0; i < height-1;i++){
            scoretotal += euclideanDistance(inputGL[i], inputGL[i+1]);
        }
        return scoretotal;
    }
    public static int breakkey(int[][] inputGL) {
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



    /**

     * Convertit une image RGB en niveaux de gris (GL).

     * @param inputRGB image d'entrée en RGB

     * @return tableau 2D des niveaux de gris (0-255)

     */

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

                // luminance simple (évite float)

                int gray = (r * 299 + g * 587 + b * 114) / 1000;

                outGL[y][x] = gray;

            }

        }

        return outGL;

    }


    /**

     * Génère une permutation des entiers 0..size-1 en fonction d'une clé.

     * @param size taille de la permutation

     * @param key clé de génération (15 bits)

     * @return tableau de taille 'size' contenant une permutation des entiers 0..size-1

     */

    public static int[] generatePermutation(int size, int key){

        int[] scrambleTable = new int[size];

        for (int i = 0; i < size; i++) scrambleTable[i] = scrambledId(i, size, key);

        return scrambleTable;

    }


    /**

     * Mélange les lignes d'une image selon une permutation donnée.

     * @param inputImg image d'entrée

     * @param perm permutation des lignes (taille = hauteur de l'image)

     * @return image de sortie avec les lignes mélangées

     */

    public static BufferedImage scrambleLines(BufferedImage inputImg, int[] perm){

        int width = inputImg.getWidth();

        int height = inputImg.getHeight();

        if (perm.length != height) throw new IllegalArgumentException("Taille d'image <> taille permutation");


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

        if (perm.length != height) throw new IllegalArgumentException("Taille d'image <> taille permutation");

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

        /**

         * Renvoie la position de la ligne id dans l'image brouillée.

         * @param id  indice de la ligne dans l'image claire (0..size-1)

         * @param size nombre total de lignes dans l'image

         * @param key clé de brouillage (15 bits)

         * @return indice de la ligne dans l'image brouillée (0..size-1)

         */

    public static int scrambledId(int id, int size, int key) {
        int s = key & 0x7F;
        int r = (key >> 7) & 0xFF;

        return (r + (2 * s + 1) * id) % size;
    }



}