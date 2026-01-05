import javax.imageio.ImageIO;
//modifier des pixels
import java.awt.image.BufferedImage;
//creer des fichiers ou repertoire
import java.io.File;
//gere les erreurs lié aux fichiers
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;




Public class Profiler{

    public static void main(String[] args) throws IOException {

        if (args.length < 4) {

            System.err.println("Usage: java Brouillimg <image_claire> <clé (optionel si cassage)> <scramble(0), unscramble(1), cassage euclidien(2), cassage Pearson(3), ou cassage nul(4)> <choix de l'annalyse> [image_sortie]");

            System.exit(1);

        }


        switch (args[3]) {
            case 1:
                analyse(args[0], args[1], args[2]);
                break;
            default:
                System.out.println("valeur incorrecte pour l'argument 3");
                System.exit(1);
        }

    }


    public static double analyse(Function<Double, Double> oneMethod, double x){

    }

    public
}