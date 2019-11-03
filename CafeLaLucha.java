import java.util.Scanner;

public class CafeLaLucha {
   public static void main( String [] args ) {

      //menú
      System.out.println( "________________________" );
      System.out.println( "  1  Americano   20.00  " );
      System.out.println( "  2  Expreso     25.00  " );
      System.out.println( "  3  Capuchino   35.00  " );
      System.out.println( "________________________" ); 

   double precio1 = 20.00;
   double precio2 = 25.00;
   double precio3 = 35.00;
   
   System.out.println( "Ingrese el número de su bebiba: " );
      int bebida = new Scanner(System.in).nextInt();

   if (bebida == 1)
      System.out.println( "________________________" );
      System.out.println( "  1  Americano   20.00  " );
      System.out.println( "  2  Expreso     25.00  " );
      System.out.println( "  3  Capuchino   35.00  " );
      System.out.println( "________________________" );
      System.out.println( "Total = " + precio1 ); 
   else if (bebida == 2)
      System.out.println( "________________________" );
      System.out.println( "  1  Americano   20.00  " );
      System.out.println( "  2  Expreso     25.00  " );
      System.out.println( "  3  Capuchino   35.00  " );
      System.out.println( "________________________" );
      System.out.println( "Total = " + precio2 ); 
   else if (bebida == 3)
      System.out.println( "________________________" );
      System.out.println( "  1  Americano   20.00  " );
      System.out.println( "  2  Expreso     25.00  " );
      System.out.println( "  3  Capuchino   35.00  " );
      System.out.println( "________________________" );
      System.out.println( "Total = " + precio3 ); 
  }
}
