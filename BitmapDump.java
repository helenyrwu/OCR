/*
 * @author Helen Wu
 * 
 * @version 04/14/15
 * 
 * This file contains the class BitmapDump which will open bitmaps and extract the bits
 * as an array of integers. This file also contains the final class RgbQuad that holds
 * the three int values for red, green, and blue, as well as an int value reserved.
 * 
 * To use, run the main method. The default bitmap input file name is "test1.bmp" 
 * and the default bitmap output file name is "test2.bmp". The program will write
 * the input bitmap out to imageArray and print imageArray out to the output 
 * bitmap file. If the user wishes to choose the input and output file names, 
 * put an array of two strings as the parameter of the main function. The 
 * first element should be the desired input name and the second should be 
 * the desired output name.
 * 
 * Classes in the file:
 *  BitmapDump
 *  RgbQuad
 *  
 * Methods in this file:
 *  int     swapInt(int v)
 *  int     swapShort(int v)
 *  RgbQuad pelToRGBQ(int pel)
 *  int     rgbqToPel(int red, int green, int blue, int reserved)
 *  RgbQuad pelToRGB(int pel)
 *  int     rgbToPel(int red, int green, int blue)
 *  int     colorToGrayscale(int pel)
 *  void    main(String[] args)
 *  public void readColorTable(int numberOfColors, int[] colorPallet, int[] rgbQuad, 
 *                             DataInputStream in)
 *  public void readFileHeader(DataInputStream in)
 *  public void readInfoHeader(DataInputStream in)
 *  public int setNumColors()
 *  public int[] createArray(DataInputStream in, int i, int j, int k, int[] colorPallet, 
                             int[] rgbQuad)
 *  public void printImageBytes(int iBytesPerRow)
 *  public void bitmapToFile(int iDeadBytes, String outFileName, int[] rgbQuad)
 *  
 * Notes on reading bitmaps:
 *
 * The BMP format assumes an Intel integer type (little endian), however, the Java virtual machine
 * uses the Motorola integer type (big endian), so we have to do a bunch of byte swaps to get things
 * to read and write correctly. Also note that many of the values in a bitmap header are unsigned
 * integers of some kind and Java does not know about unsigned values, except for reading in
 * unsigned byte and unsigned short, but the unsigned int still poses a problem.
 * We don't do any math with the unsigned int values, so we won't see a problem.
 *
 * Bitmaps on disk have the following basic structure
 *  BITMAPFILEHEADER (may be missing if file is not saved properly by the creating application)
 *  BITMAPINFO -
 *        BITMAPINFOHEADER
 *        RGBQUAD - Color Table Array (not present for true color images)
 *  Bitmap Bits in one of many coded formats
 *
 *  The BMP image is stored from bottom to top, meaning that the first scan line in the file is 
 *  the last scan line in the image.
 *
 *  For ALL images types, each scan line is padded to an even 4-byte boundary.
 *  
 *  For images where there are multiple pels per byte, the left side is the high order element and 
 *  the right is the low order element.
 */

import java.io.*;
import java.lang.Exception.*;
/*
 * This BitmapDump class gives the capability to open bitmaps and extract the bits as an array 
 * of integers if the given file is a bitmap file type. The default bitmap input file name is 
 * "test1.bmp" and the default bitmap output file name is "test2.bmp". 
 * 
 * The main program will extract the input bitmap by its pixel elements to imageArray and 
 * print imageArray out to an output bitmap file. 
 * 
 * If the user wishes to choose the input and output file names, put an 
 * array of two strings as the parameter of the main function. The first element 
 * should be the desired input name and the second should be the desired output name.
 * The file header of the selected file is read in first. If the file is a bmp file,
 * the program will procede to read the rest of the file info header and the rest of
 * the pixel elements. If the array is small (less than 30 x 30), the program will print 
 * the hex values to the console. The color table is created based on the number of colors 
 * in the image, which is given but the bitmap file header. Then the array is saved as 
 * a 24-bit true color bitmap. 
 * 
 * in Windows on a 32 bit processor...
 *  DWORD is an unsigned 4 byte integer
 *  WORD is an unsigned 2 byte integer
 *  LONG is a 4 byte signed integer
 *
 * in Java we have the following sizes:
 *
 * byte
 *   1 signed byte (two's complement). Covers values from -128 to 127.
 *
 * short
 *   2 bytes, signed (two's complement), -32,768 to 32,767
 *
 * int
 *   4 bytes, signed (two's complement). -2,147,483,648 to 2,147,483,647.
 *   Like all numeric types ints may be cast into other numeric types (byte, short, long, 
 *   float, double).
 *   When lossy casts are done (e.g. int to byte) the conversion is done modulo the 
 *   length of the smaller type.
 * 
 * 
 */
public class BitmapDump
{
   static final int QUAD = 4;
   
   static final int BLUE = 0;
   static final int GREEN = 1;
   static final int RED = 2;
   static final int RESERVED = 3;
   
   static final int BYTES_PER_ROW = 0;
   static final int DEAD_BYTES = 1;
   static final int NUM_OUTPUTS = 2;
   
   static final int BM = 0x4D42;
   
   static final int COLOR_MEM = 256;
   
   static final int PELS_PER_BYTE1 = 8;      // case1
   static final int PELS_PER_BYTE2 = 4;      // case2
   static final int PELS_PER_BYTE4 = 2;      // case4
   
   static final int BYTE = 8;                //number of bits in byte
   
   static final double RED_MULT = 0.3;
   static final double GREEN_MULT = 0.589;
   static final double BLUE_MULT = 0.11;
   
   static final int MAX_DIM = 33;
   
   static final int CENTER = 50;
   static final int DIMENSION = 101;
   
   static final int ROW_INDEX = 0;
   static final int COL_INDEX = 1;
   
   /*
    * BITMAPFILEHEADER
    */
   static int bmpFileHeader_bfType;          // WORD
   static int bmpFileHeader_bfSize;          // DWORD
   static int bmpFileHeader_bfReserved1;     // WORD
   static int bmpFileHeader_bfReserved2;     // WORD
   static int bmpFileHeader_bfOffBits;       // DWORD
   
   /*
    * BITMAPINFOHEADER
    */
   static int bmpInfoHeader_biSize;          // DWORD
   static int bmpInfoHeader_biWidth;         // LONG
   static int bmpInfoHeader_biHeight;        // LONG
   static int bmpInfoHeader_biPlanes;        // WORD
   static int bmpInfoHeader_biBitCount;      // WORD
   static int bmpInfoHeader_biCompression;   // DWORD
   static int bmpInfoHeader_biSizeImage;     // DWORD
   static int bmpInfoHeader_biXPelsPerMeter; // LONG
   static int bmpInfoHeader_biYPelsPerMeter; // LONG
   static int bmpInfoHeader_biClrUsed;       // DWORD
   static int bmpInfoHeader_biClrImportant;  // DWORD
   
   
   /*
    * The true color pels
    */
   static int[][] imageArray;
   
   /* 
    * If bmpInfoHeader_biHeight is negative then the image is a top down DIB. 
    * This flag is used to identify it as such. Note that when the image is saved, 
    * it will be written out in the usual inverted format with a positive 
    * bmpInfoHeader_biHeight value.
    */
   static boolean topDownDIB = false;
   
   /*
    * This method is used to go between little and big endian integer formats.
    * 
    * The highest order byte is moved to the lowest order position.
    * The second highest order byte is moved to the second lowest order position.
    * The third highest order byte is moved to the third lowest order position.
    * The lowest order byte is moved to the highest order position.
    * 
    * @param v  integer in little or big endian format to be switched
    * 
    * @return   the swapped integer representing the opposite endian integer format
    */
   public int swapInt(int v)
   {
      return  (v >>> 3 * BYTE) | (v << 3 * BYTE) | ((v << BYTE) & 0x00FF0000) | 
              ((v >> BYTE) & 0x0000FF00);
   }

   /*
    * This method is used to go between little and big endian short integer formats.
    * 
    * The higher and lower order bytes are swapped.
    * 
    * @param v  integer in little or big endian format to be switched
    * 
    * @return   the swapped integer representing the opposite endian short integer format
    */
   public int swapShort(int v)
   {
      return  ((v << BYTE) & 0xFF00) | ((v >> BYTE) & 0x00FF);
   }
   
   /*
    * The rgbqToPel method takes red, green and blue color values plus an additional 
    * byte and returns a single 32-bit integer color.
    * See pelToRGBQ(int pel) to go the other way.
    * 
    */
   public int rgbqToPel(int red, int green, int blue, int reserved)
   {
      return (reserved << 3 * BYTE) | (red << 2 * BYTE) | (green << BYTE) | blue;
   }
   
   /*
    * Method pelToRGBQ accepts an integer (32 bit) picture element and returns the red, 
    * green and blue colors. Unlike pelToRGB, this method also extracts the most 
    * significant byte and populates the reserved element of RgbQuad. It returns 
    * an RgbQuad object. 
    * 
    * See rgbqToPel(int red, int green, int blue, int reserved) to go the the other way. 
    */
   public RgbQuad pelToRGBQ(int pel)
   {
      RgbQuad rgbq = new RgbQuad();

      rgbq.blue     =  pel & 0x00FF;
      rgbq.green    = (pel >> BYTE)  & 0x00FF;
      rgbq.red      = (pel >> 2 * BYTE) & 0x00FF;
      rgbq.reserved = (pel >> 3 * BYTE) & 0x00FF;
            
      return rgbq;
   }
   
   /*
    * Method pelToRGB accepts an integer (32 bit) picture element and returns the 
    * red, green and blue colors as an RgbQuad object. 
    * 
    * See rgbToPel(int red, int green, int blue) to go the the other way. 
    */
   public RgbQuad pelToRGB(int pel)
   {
      RgbQuad rgb = new RgbQuad();

      rgb.reserved = 0;

      rgb.blue  =  pel & 0x00FF;
      rgb.green = (pel >> BYTE)  & 0x00FF;
      rgb.red   = (pel >> 2*BYTE) & 0x00FF;
        
      return rgb;
   }

   /*
    * The rgbToPel method takes red, green and blue color values and returns a 
    * single 32-bit integer color.
    * 
    * See pelToRGB(int pel) to go the other way.
    */
   public int rgbToPel(int red, int green, int blue)
   {
      return (red << 2*BYTE) | (green << BYTE) | blue;
   }

   /*
    * Y = 0.3RED+0.59GREEN+0.11Blue
    * The colorToGrayscale method takes a color picture element (pel) 
    * and returns the gray scale pel.
    */
   public int colorToGrayscale(int pel)
   {
      RgbQuad rgb = pelToRGB(pel);
    
      int lum = (int)Math.round(RED_MULT * (double)rgb.red + GREEN_MULT * 
                     (double)rgb.green + BLUE_MULT * (double)rgb.blue);

      return rgbToPel(lum, lum, lum);
   }
   
   /*
    * Converts color picture element (pel) to grayscale, then takes 
    * least significant byte to return an 8-bit value
    */
   public int colorTo8BitGrayscale(int pel)
   {
      int gPel = colorToGrayscale(pel);
      
      return ~gPel & 0xFF;
   }
   
   /*
    * Finds center of mass using equations
    * 
    * x_centerofmass = sum(x_i*grayscale_i)/sum(grayscale_i)
    * y_centerofmass = sum(y_i*grayscale_i)/sum(grayscale_i)
    */
   public int[] findCOM()
   {
      double rowSum, colSum, graySum;
      int rowCOM, colCOM;
      int[] centerOfMass = new int[NUM_OUTPUTS];
      
      rowSum = colSum = graySum = 0;
      
      for (int iRow = 0; iRow < bmpInfoHeader_biHeight; iRow++)
      {
         for (int iColumn = 0; iColumn < bmpInfoHeader_biWidth; iColumn++)
         {
            rowSum += iRow * imageArray[iRow][iColumn];
            colSum += iColumn * imageArray[iRow][iColumn];
            
            graySum += imageArray[iRow][iColumn];
         }
      }
      
      rowCOM = (int)(rowSum / graySum);
      colCOM = (int)(colSum / graySum);
      
      centerOfMass[ROW_INDEX] = rowCOM; //MAGIC NUMBERS?????
      centerOfMass[COL_INDEX] = colCOM;
      
      return centerOfMass;
   }
   
   /*
    * Finds and returns difference in rows and columns between 
    * current center of mass and the desired center of mass (50, 50).
    */
   public int[] findDifferenceCOM(int[] centerOfMass)
   {
      int[] deltaCOM = new int[NUM_OUTPUTS];
      
      deltaCOM[ROW_INDEX] = CENTER - centerOfMass[ROW_INDEX];
      deltaCOM[COL_INDEX] = CENTER - centerOfMass[COL_INDEX];
      
      return deltaCOM;
   }
   
   /*
    * Returns imageArray with the image shifted so that the 
    * center of mass is at the center of the 101 by 101-pixel
    * image, at (50, 50). 
    * 
    * Because Java initializes all values in an integer array as 0, 
    * the default pixel value 0 creates a black pixel
    * on the shifted image wherever there is no corresponding pixel 
    * in (out of bounds of) the unshifted image.
    * 
    * 
    * At the end, the new image array will be looped through, and
    * any values that are still null will be set to be the dark 
    * background.
    */
   public void shiftImage(int[] deltaCOM)
   {
      int[][] newImageArray = new int[bmpInfoHeader_biHeight][bmpInfoHeader_biWidth];
      int startRow, endRow, startCol, endCol;
      
      startRow = endRow = startCol = endCol = 0;
      
      if(deltaCOM[ROW_INDEX] > 0)
      {
         startRow = 0;
         endRow = bmpInfoHeader_biHeight - deltaCOM[ROW_INDEX];
      }
      else
      {
         startRow = -deltaCOM[ROW_INDEX];
         endRow = bmpInfoHeader_biHeight;
      }
      
      if(deltaCOM[COL_INDEX] > 0)
      {
         startCol = 0;
         endCol = bmpInfoHeader_biWidth - deltaCOM[COL_INDEX];
      }
      else
      {
         startCol = -deltaCOM[COL_INDEX];
         endCol = bmpInfoHeader_biWidth;
      }
      
      for (int iRow = startRow; iRow < endRow; iRow++)
      {
         for (int iColumn = startCol; iColumn < endCol; iColumn++)
         {
            newImageArray[iRow + deltaCOM[ROW_INDEX]][iColumn + deltaCOM[COL_INDEX]]
               = imageArray[iRow][iColumn];
         }
      }
      
      imageArray = newImageArray;
      
      return;
   }
   
   /*
    * This method runs the program. First, it reads in the file header from the file with
    * the default name "bitmap.1" and converts it to big endian. From the header, the
    * program determines whether the file is indeed a bitmap (bmpFileHeader_bfType == BM).
    * If not, the program does not continue and prints to the console that the file is not
    * a bitmap type file. If the file is a bitmap, the info header is read in and 
    * converted to big endian. The height of the bmp is set to positive if the given 
    * height in the header is negative so that it can be used to create the array.
    * The number of colors is determined based based on information from the file info
    * header. The color table is read in, and then the bitmap pixel elements are 
    * converted into an array of integers. Finally, the program prints the array
    * if it is small enough and creates an output bitmap file that should be the same
    * as the input if the program is implemented correctly.
    * 
    * 
    * The variable iDeadBytes serves to count the number of dead
    * bytes needed to ensure that the number of bytes per row is a multiple of four.
    * 
    */
   public static void main(String[] args) throws IOException
   {
      String inFileName, outFileName;
      int i, j, k;
      int numberOfColors;
      int pel;
      int iBytesPerRow, iPelsPerRow, iDeadBytes;
      
      i = j = k = iBytesPerRow = iDeadBytes = 0;  // initialization
      
      /* 
       * int[] rgbQuad -- array containing values which describes a color
       * consisting of relative intensities of red, green, and blue
       * 
       * rgbQuad[BLUE] = blue, rgbQuad[GREEN] = green, 
       * rgbQuad[RED] = red, rgbQuad[RESERVED] = reserved
       *            
       * Reserved slot not used in the method
       * 
       * BLUE     = 0    - index for blue integer
       * GREEN    = 1    - index for green integer
       * RED      = 2    - index for red integer
       * RESERVED = 3    - index for reserved integer
       */
      int[] rgbQuad = new int[QUAD]; 
      
      /*
       * Creates the color table, reserving space for the largest possible
       * color table
       * 
       * final int COLOR_MEM = 256;
       */
      int[] colorPallet = new int[COLOR_MEM];

      BitmapDump dibdumper = new BitmapDump(); // needed to get to the byte- 
                                               // swapping methods

      if (args.length > 0)
         inFileName = args[0];
      else
         inFileName = "/Users/helen/Dropbox/TimesNewRoman/A.bmp";

      if (args.length > 1)
         outFileName = args[1];
      else
         outFileName = "/Users/helen/Dropbox/TimesNewRoman/testomg.bmp";

      try // lots of things can go wrong when doing file i/o
      {
           
         /*
          * Open the file that is the first command line parameter
          */
         FileInputStream fstream = new FileInputStream(inFileName);

            
         DataInputStream in = new DataInputStream(fstream); // Convert our input stream to 
                                                            // a DataInputStream
         
            
         dibdumper.readFileHeader(in);                      // Read and convert bitmap 
                                                            // file header to big endian
         
         if(bmpFileHeader_bfType == BM)                     //Confirms that file
                                                            //is a bitmap type
         {
            
            dibdumper.readInfoHeader(in);                   // Read and convert bitmap 
                                                            // info header to big endian
            
            /* 
             * Since we use the height to crate arrays, it cannot have a negative a value. 
             * If the height field is less than zero, then make it positive and set the 
             * topDownDIB flag to TRUE so we know that the image is
             * stored on disc upsidedown (which means it is actually rightside up).
             */
            if (bmpInfoHeader_biHeight < 0)
            {
               topDownDIB = true;
               bmpInfoHeader_biHeight = -bmpInfoHeader_biHeight;
            }
            
            /*
             * Determine the number of colors in the default color table
             */
            numberOfColors = dibdumper.setNumColors();
         
            /*
             * biClrUsed -  Specifies the number of color indexes in the color table that 
             * are actually used by the bitmap.
             * 
             *     If this value is zero, the bitmap uses the maximum number of colors 
             *     corresponding to the value of the biBitCount member for the compression mode 
             *     specified by biCompression.
             *     
             *     If biClrUsed is nonzero and the biBitCount member is less than 16, the 
             *     biClrUsed  member specifies the actual number of colors the graphics 
             *     engine or device driver accesses.
             *     
             *     If biBitCount is 16 or greater, the biClrUsed member specifies the size 
             *     of the color table used to optimize performance of the system color palettes.
             *     
             *     If biBitCount equals 16 or 32, the optimal color palette starts immediately 
             *     following the three DWORD masks.
             *     
             *     If the bitmap is a packed bitmap (a bitmap in which the bitmap array 
             *     immediately follows the BITMAPINFO header and is referenced by a single 
             *     pointer), the biClrUsed member must be either zero or the actual size of 
             *     the color table.
             */
            if (bmpInfoHeader_biClrUsed > 0) 
            {
               numberOfColors = bmpInfoHeader_biClrUsed;
            }
            
            /*
             * The following loop reads in the color table (or not if numberOfColors
             * is zero).
             */
            dibdumper.readColorTable(numberOfColors, colorPallet, rgbQuad, in);
            
            /*
             * dataArray is an array holding iBytesPerRow and iDeadBytes
             */
            int dataArray[] = dibdumper.createArray(in, i, j, k, colorPallet, rgbQuad);
            iBytesPerRow = dataArray[BYTES_PER_ROW];
            iDeadBytes = dataArray[DEAD_BYTES];
            
            in.close();
            fstream.close();
         }
         
         else
         {
            throw new IOException("Not a bitmap file type.");
         }
      } // try
      
      catch (Exception e)
      {
         System.err.println("File input error" + e);
      }
         
      dibdumper.printImageBytes(iBytesPerRow);
         
      dibdumper.bitmapToFile(iDeadBytes, outFileName, rgbQuad);
     
      return;
   }
   
   /*
    * This method reads in the color table to the array colorPallet given
    * the number of colors in the color table. It builds the color from the RGB 
    * values. Since we declared the rgbQuad values to be int, we can shift and 
    * then OR the values to build up the color.
    * 
    * @param numberOfColors   number of colors in the color table
    * @param colorPallet      array where the color table is stored
    * @param rgbQuad          stores the RGB values
    * @param in               DataInputStream from the input file
    * 
    */
   public void readColorTable(int numberOfColors, int[] colorPallet, int[] rgbQuad, 
                              DataInputStream in) throws IOException
   {
      for (int i = 0; i < numberOfColors; ++i)
      {
         rgbQuad[BLUE]      = in.readUnsignedByte(); // lowest byte in the color
         rgbQuad[GREEN]     = in.readUnsignedByte();
         rgbQuad[RED]       = in.readUnsignedByte(); // highest byte in the color
         rgbQuad[RESERVED]  = in.readUnsignedByte();
      
         /*
          * Build the color from the RGB values. Since we declared the rgbQuad 
          * values to be int, we can shift and then OR the values to build up the 
          * color. 
          * Since we are reading one byte at a time, there are no "endian" issues.
          */
      
         colorPallet[i] = (rgbQuad[RED] << 2 * BYTE) | (rgbQuad[GREEN] << BYTE) | 
                           rgbQuad[BLUE] ;
                                
         // System.out.printf("DEBUG: Color Table = %d, %06X\n", i, colorPallet[i]);
               
      }  // for (i = 0; i < numberOfColors; ++i)
      
      return;
   }
   
   /*
    * Read in BITMAPFILEHEADER and convert it to big endian.
    * 
    * bfType
    *    Specifies the file type. It must be set to the signature word BM (0x4D42) to 
    *    indicate bitmap.
    * bfSize
    *    Specifies the size, in bytes, of the bitmap file.
    * bfReserved1
    *    Reserved; set to zero
    * bfReserved2
    *    Reserved; set to zero
    * bfOffBits
    *    Specifies the offset, in bytes, from the BITMAPFILEHEADER structure to the 
    *    bitmap bits
    *    
    * @param in    DataInputStream for the input bitmap
    */
   public void readFileHeader(DataInputStream in) throws IOException
   {
      bmpFileHeader_bfType      = this.swapShort(in.readUnsignedShort());    // WORD
      bmpFileHeader_bfSize      = this.swapInt(in.readInt());                // DWORD
      bmpFileHeader_bfReserved1 = this.swapShort(in.readUnsignedShort());    // WORD
      bmpFileHeader_bfReserved2 = this.swapShort(in.readUnsignedShort());    // WORD
      bmpFileHeader_bfOffBits   = this.swapInt(in.readInt());                // DWORD
   
      System.out.printf("bfType=%2X bfSize=%d bfReserved1=%h bfReserved2=%h "+ 
                        "bfOffBits=%d\n",
                        bmpFileHeader_bfType,
                        bmpFileHeader_bfSize,
                        bmpFileHeader_bfReserved1,
                        bmpFileHeader_bfReserved2,
                        bmpFileHeader_bfOffBits);
                        
      return;
   }
   
   /*
    * Read in information about the bitmap from the bitmap info header and convert
    * to big endian.
    * 
    * biSize
    *     Specifies the size of the structure, in bytes.
    *     This size does not include the color table or the masks mentioned in the 
    *       biClrUsed member.
    *     See the Remarks section for more information.
    * biWidth
    *     Specifies the width of the bitmap, in pixels.
    * biHeight
    *     Specifies the height of the bitmap, in pixels.
    *     If biHeight is positive, the bitmap is a bottom-up DIB and 
    *       its origin is the lower left corner.
    *     If biHeight is negative, the bitmap is a top-down DIB and 
    *       its origin is the upper left corner.
    *     If biHeight is negative, indicating a top-down DIB, 
    *       biCompression must be either BI_RGB or BI_BITFIELDS. 
    *       Top-down DIBs cannot be compressed.
    * biPlanes
    *     Specifies the number of planes for the target device.
    *     This value must be set to 1.
    * biBitCount
    *    Specifies the number of bits per pixel.
    *     The biBitCount member of the BITMAPINFOHEADER structure determines the number of 
    *     bits that define each pixel and the maximum number of colors in the bitmap.
    *     
    *     This member must be one of the following values.
    *     Value     Description
    *     1       The bitmap is monochrome, and the bmiColors member contains two entries.
    *             Each bit in the bitmap array represents a pixel. The most significant 
    *             bit is to the left in the image. 
    *             If the bit is clear, the pixel is displayed with the color of the first 
    *             entry in the bmiColors table.
    *             If the bit is set, the pixel has the color of the second entry in 
    *             the table.
    *                
    *     2       The bitmap has four possible color values.  The most significant 
    *             half-nibble is to the left in the image.
    *             
    *     4       The bitmap has a maximum of 16 colors, and the bmiColors member 
    *             contains up to 16 entries.
    *             Each pixel in the bitmap is represented by a 4-bit index into the 
    *             color table. The most significant nibble is to the left in the image.
    *             For example, if the first byte in the bitmap is 0x1F, the byte 
    *             represents two pixels. The first pixel contains the color in the 
    *             second table entry, and the second pixel contains the color in the 
    *             sixteenth table entry.
    *             
    *     8       The bitmap has a maximum of 256 colors, and the bmiColors member 
    *             contains up to 256 entries. In this case, each byte in the 
    *             array represents a single pixel.
    *                
    *     24      The bitmap has a maximum of 2^24 colors, and the bmiColors member 
    *             is NULL.
    *             Each 3-byte triplet in the bitmap array represents the relative 
    *             intensities of blue, green, and red, respectively, for a pixel.
    *             The bmiColors color table is used for optimizing colors used on 
    *             palette-based devices, and must contain the number of entries 
    *             specified by the biClrUsed member of the BITMAPINFOHEADER.
    *                
    *     32      The bitmap has a maximum of 2^32 colors. If the biCompression member 
    *             of the BITMAPINFOHEADER is BI_RGB, the bmiColors member is NULL. 
    *             Each DWORD in the bitmap array represents the relative intensities 
    *             of blue, green, and red, respectively, for a pixel. The high byte 
    *             in each DWORD is not used. The bmiColors color table is used for 
    *             optimizing colors used on palette-based devices, and must contain 
    *             the number of entries specified by the biClrUsed member of the 
    *             BITMAPINFOHEADER.
    *             If the biCompression member of the BITMAPINFOHEADER is BI_BITFIELDS, 
    *             the bmiColors member contains three DWORD color masks that specify the 
    *             red, green, and blue components, respectively, of each pixel.
    *             Each DWORD in the bitmap array represents a single pixel.
    *             
    * biCompression
    *     Specifies the type of compression for a compressed bottom-up bitmap 
    *     (top-down DIBs cannot be compressed). This member can be one of the 
    *     following values.
    *     
    *     Value               Description
    *     BI_RGB              An uncompressed format.
    *     BI_BITFIELDS        Specifies that the bitmap is not compressed and that the 
    *                         color table consists of three DWORD color masks that specify
    *                         the red, green, and blue components of each pixel.
    *                         This is valid when used with 16- and 32-bpp bitmaps.
    *                         This value is valid in Windows Embedded CE versions 2.0 and 
    *                         later.
    *     BI_ALPHABITFIELDS   Specifies that the bitmap is not compressed and that the 
    *                         color table consists of four DWORD color masks that specify 
    *                         the red, green, blue, and alpha components of each pixel.
    *                         This is valid when used with 16- and 32-bpp bitmaps.
    *                         This value is valid in Windows CE .NET 4.0 and later.
    *                         You can OR any of the values in the above table with 
    *                         BI_SRCPREROTATE to specify that the source DIB section has 
    *                         the same rotation angle as the destination.
    * biSizeImage
    *     Specifies the size, in bytes, of the image. This value will be the number of 
    *     bytes in each scan line which must be padded to insure the line is a multiple of 
    *     4 bytes (it must align on a DWORD boundary) times the number of rows.
    *     This value may be set to zero for BI_RGB bitmaps (so you cannot be sure it will 
    *     be set).
    * biXPelsPerMeter
    *     Specifies the horizontal resolution, in pixels per meter, of the target device 
    *     for the bitmap.
    *     An application can use this value to select a bitmap from a resource group that 
    *     best matches the characteristics of the current device.
    * biYPelsPerMeter
    *     Specifies the vertical resolution, in pixels per meter, of the target device for 
    *     the bitmap
    * biClrUsed
    *     Specifies the number of color indexes in the color table that are actually used 
    *     by the bitmap.
    *     If this value is zero, the bitmap uses the maximum number of colors 
    *     corresponding to the value of the biBitCount member for the compression mode 
    *     specified by biCompression.
    *     If biClrUsed is nonzero and the biBitCount member is less than 16, the biClrUsed 
    *     member specifies the actual number of colors the graphics engine or device 
    *     driver accesses.
    *     If biBitCount is 16 or greater, the biClrUsed member specifies the size of the 
    *     color table used to optimize performance of the system color palettes.
    *     If biBitCount equals 16 or 32, the optimal color palette starts immediately 
    *     following the three DWORD masks.
    *     If the bitmap is a packed bitmap (a bitmap in which the bitmap array immediately
    *     follows the BITMAPINFO header and is referenced by a single pointer), the
    *     biClrUsed member must be either zero or the actual size of the color table.
    * biClrImportant
    *     Specifies the number of color indexes required for displaying the bitmap.
    *     If this value is zero, all colors are required.
    *     
    *     
    * Remarks
    *
    * The BITMAPINFO structure combines the BITMAPINFOHEADER structure and a color table 
    * to provide a complete definition of the dimensions and colors of a DIB.
    * An application should use the information stored in the biSize member to locate the 
    * color table in a BITMAPINFO structure, as follows.
    *
    * pColor = ((LPSTR)pBitmapInfo + (WORD)(pBitmapInfo->bmiHeader.biSize));
    * 
    * @param in   the DataInputStream for the input bitmap
    */
   public void readInfoHeader(DataInputStream in) throws IOException
   {
      bmpInfoHeader_biSize          = this.swapInt(in.readInt());             // DWORD
      bmpInfoHeader_biWidth         = this.swapInt(in.readInt());             // LONG
      bmpInfoHeader_biHeight        = this.swapInt(in.readInt());             // LONG
      bmpInfoHeader_biPlanes        = this.swapShort(in.readUnsignedShort()); // WORD
      bmpInfoHeader_biBitCount      = this.swapShort(in.readUnsignedShort()); // WORD
      bmpInfoHeader_biCompression   = this.swapInt(in.readInt());             // DWORD
      bmpInfoHeader_biSizeImage     = this.swapInt(in.readInt());             // DWORD
      bmpInfoHeader_biXPelsPerMeter = this.swapInt(in.readInt());             // LONG
      bmpInfoHeader_biYPelsPerMeter = this.swapInt(in.readInt());             // LONG
      bmpInfoHeader_biClrUsed       = this.swapInt(in.readInt());             // DWORD
      bmpInfoHeader_biClrImportant  = this.swapInt(in.readInt());             // DWORD

      System.out.printf("biSize=%d\nbiWidth=%d\nbiHeight=%d\nbiPlanes=%d\n" + 
                        "biBitCount=%d\nbiCompression=%d\nbiSizeImage=%d\n" +
                        "biXPelsPerMeter=%d\nbiYPelsPerMeter=%d\nbiClrUsed=%d\n" + 
                        "biClrImportant=%d\n",
                 bmpInfoHeader_biSize,
                 bmpInfoHeader_biWidth,
                 bmpInfoHeader_biHeight,
                 bmpInfoHeader_biPlanes,
                 bmpInfoHeader_biBitCount,
                 bmpInfoHeader_biCompression,
                 bmpInfoHeader_biSizeImage,
                 bmpInfoHeader_biXPelsPerMeter,
                 bmpInfoHeader_biYPelsPerMeter,
                 bmpInfoHeader_biClrUsed,
                 bmpInfoHeader_biClrImportant);

      System.out.printf("\n");
      
      return;
   }
   
   /*
    * Determines and returns the number of colors in the color table
    * given the number of bits per pixel.
    * 
    * @return   number of colors in the color table
    */
   public int setNumColors()
   {
      int numberOfColors;
      
      switch (bmpInfoHeader_biBitCount) 
      {
         case 1:
            numberOfColors = 2;
            break;
         case 2:
            numberOfColors = 4;
            break;
         case 4:
            numberOfColors = 16;
            break;
         case 8:
            numberOfColors = 256;
            break;
         default:
            numberOfColors = 0; // no color table
      }
   
      System.out.printf("Color Depth = %d, %d\n", bmpInfoHeader_biBitCount, 
                                                  numberOfColors);
                              
      return numberOfColors;
   }
   
   
   /*
    * Now for the fun part. We need to read in the rest of the bit map, but how we 
    * interpret the values depends on the color depth. The elements of imageArray 
    * are set to the bitmap.
    *
    * numberOfColors = 2:   Each bit is a pel, so there are 8 pels per byte. The Color 
    *                       Table has only two values for "black" and "white"
    * numberOfColors = 4:   Each pair of bits is a pel, so there are 4 pels per byte. The 
    *                       Color Table has only four values
    * numberOfColors = 16;  Each nibble (4 bits) is a pel, so there are 2 pels per byte. 
    *                       The Color Table has 16 entries.
    * numberOfColors = 256; Each byte is a pel and the value maps into the 256 byte Color 
    *                       Table.
    *
    * Any other value is read in as "true" color.
    *
    * The BMP image is stored from bottom to top, meaning that the first scan line is the 
    * last scan line in the image.
    *
    * The rest is the bitmap. Use the height and width information to read it in. And as 
    * I mentioned before....
    * In the 24-bit format, each pixel in the image is represented by a series of three 
    * bytes of RGB stored as BRG.
    * 
    * For ALL image types each scan line is padded to an even 4-byte boundary.
    * 
    * @param in            DataInputStream to read in the input bmp file
    * @param i             index to loop over each row
    * @param j             index to loop over bytes in each row
    * @param k             index to loop over the pixel elements in each byte
    * @param colorPallet   array giving the color table
    * @param rgbQuad       aray giving the RGB elements of a color
    * 
    * @return              output array so that the method can return multiple 
    *                      outputs 
    *                      returns iBytesPerRow and and iDeadBytes
    *
    */
   public int[] createArray(DataInputStream in, int i, int j, int k, int[] colorPallet, 
                           int[] rgbQuad) throws IOException
   {
      int pel, gPel;
      int iBytesPerRow, iTrailingBits, iDeadBytes, iByteVal, iColumn, iPelsPerRow;
      int[] outputArray;
      
      iBytesPerRow = iDeadBytes = 0;
      outputArray = new int[NUM_OUTPUTS];
      
      /*
       * Create the array for the pels
       */
      imageArray = new int[bmpInfoHeader_biHeight][bmpInfoHeader_biWidth]; 
      
      
      /*
       * I use the same loop structure for each case for clarity so you can see the 
       * similarities and differences.
       * 
       * The outer loop is over the rows (in reverse), the inner loop over the columns. 
       */
      switch (bmpInfoHeader_biBitCount)
      {
         /*
          * Each byte read in is 8 columns, so we need to break them out. We also
          * have to deal with the case where the image width is not an integer 
          * multiple of 8, in which case we will have bits from part of the 
          * remaining byte. Each color is 1 bit which is masked with 0x01.
          * The screen ordering of the pels is High-Bit to Low-Bit, so the most 
          * significant element is first in the array of pels.
          */
         case 1: // each bit is a color, so there are 8 pels per byte.  Works
         
            iBytesPerRow = bmpInfoHeader_biWidth / PELS_PER_BYTE1;
            iTrailingBits = bmpInfoHeader_biWidth % PELS_PER_BYTE1;
   
            iDeadBytes = iBytesPerRow;
            if (iTrailingBits > 0) ++iDeadBytes;
            iDeadBytes = (QUAD - iDeadBytes % QUAD) % QUAD;
   
            for (int row = 0; row < bmpInfoHeader_biHeight; ++row) // read over the rows
            {
               if (topDownDIB) i = row; else i = bmpInfoHeader_biHeight - 1 - row;
                     
               for (j = 0; j < iBytesPerRow; ++j)
               {
                  iByteVal = in.readUnsignedByte();
   
                  for (k = 0; k < PELS_PER_BYTE1; ++k)             // Get 8 pels from the one byte
                  {
                     iColumn = j * PELS_PER_BYTE1 + k;
                     
                     pel = colorPallet[(iByteVal >> ((PELS_PER_BYTE1 - 1) - k)) & 0x01];
                     gPel = colorTo8BitGrayscale(pel);
                     
                     imageArray[i][iColumn] = gPel;
                  }
                  
               } // for (j = 0; j < iBytesPerRow; ++j)
               
               if (iTrailingBits > 0) // pick up the trailing bits for images that are 
                                      // not mod 8 columns wide
               {
                  iByteVal = in.readUnsignedByte();
   
                  for (k = 0; k < iTrailingBits; ++k)
                  {
                     iColumn = iBytesPerRow * PELS_PER_BYTE1 + k;
                     
                     pel = colorPallet[(iByteVal >> ((PELS_PER_BYTE1 - 1) - k)) & 0x01];
                     gPel = colorTo8BitGrayscale(pel);
                     
                     imageArray[i][iColumn] = gPel;
                  }
                  
               } // if (iTrailingBits > 0)
   
               /*
                * Now read in the "dead bytes" to pad to a 4 byte boundary
                */
               for (j = 0; j < iDeadBytes; ++j) in.readUnsignedByte(); 
            
            } // for (int row = 0; row < bmpInfoHeader_biHeight; ++row)
            
            break;
         
         /*
          * Each byte read in is 4 columns, so we need to break them out. We also have to deal 
          * with the case where the image width is not an integer multiple of 4, in which case 
          * we will have from 2 to 6 bits of the remaining byte. Each color is 2 bits which is 
          * masked with 0x03. The screen ordering of the pels is High-Half-Nibble to 
          * Low-Half-Nibble, so the most significant element is first in the array of pels.
          */
         case 2: // 4 colors, Each byte is 4 pels (2 bits each),  Should work, not tested.
         
            iBytesPerRow = bmpInfoHeader_biWidth / PELS_PER_BYTE2;
            iTrailingBits = bmpInfoHeader_biWidth % PELS_PER_BYTE2; // 0, 1, 2 or 3

            iDeadBytes = iBytesPerRow;
            if (iTrailingBits > 0) ++iDeadBytes;
            iDeadBytes = (QUAD - iDeadBytes % QUAD) % QUAD;

            for (int row = 0; row < bmpInfoHeader_biHeight; ++row)  // read over the rows
            {
               if (topDownDIB) i = row; else i = bmpInfoHeader_biHeight - 1 - row;

               for (j = 0; j < iBytesPerRow; ++j)
               {
                  iByteVal = in.readUnsignedByte();

                  for (k = 0; k < PELS_PER_BYTE2; ++k)              // Get 4 pels from one byte
                  {
                     iColumn = j * PELS_PER_BYTE2 + k;
                     
                     /*
                      * The following line shifts 2 bits at a time and reverse order
                      */
                     pel = colorPallet[(iByteVal >> (((PELS_PER_BYTE2 - 1) - k) * 2)) & 0x03];
                     gPel = colorTo8BitGrayscale(pel);
                     
                     imageArray[i][iColumn] = gPel;
                     
                  } // for (k = 0; k < 4; ++k) 
                  
               } // for (j = 0; j < iBytesPerRow; ++j)
               
               
               if (iTrailingBits > 0) // pick up the trailing nibble for images that are not
                                      // mod 2 columns wide
               {
                  iByteVal = in.readUnsignedByte();

                  for (k = 0; k < iTrailingBits; ++k)
                  {
                     iColumn = iBytesPerRow * PELS_PER_BYTE2 + k; 
                     
                     pel = colorPallet[(iByteVal >> (((PELS_PER_BYTE2 - 1) - k) * 2)) & 0x03];
                     gPel = colorTo8BitGrayscale(pel);
                     
                     imageArray[i][iColumn] = gPel;
                  } // for (k = 0; k < iTrailingBits; ++k)
                  
               } // if (iTrailingBits > 0)
               
               /*
                * Now read in the "dead bytes" to pad to a 4 byte boundary
                */
               for (j = 0; j < iDeadBytes; ++j) in.readUnsignedByte(); 
               
            } // for (int row = 0; row < bmpInfoHeader_biHeight; ++row)
            
            break;
         
         /*
          * Each byte read in is 2 columns, so we need to break them out. We also have to 
          * deal with the case where the image width is not an integer multiple of 2, in 
          * which case we will have one nibble from part of the remaining byte. We then 
          * read in the dead bytes so that each scan line is a multiple of 4 bytes. Each 
          * color is a nibble (4 bits) which is masked with 0x0F. The screen ordering of 
          * the pels is High-Nibble Low-Nibble, so the most significant element is first 
          * in the array of pels.
          */
         case 4: // 16 colors, Each byte is two pels. Works
            iPelsPerRow   = bmpInfoHeader_biWidth;
            iBytesPerRow  = iPelsPerRow / 2;
            iTrailingBits = iPelsPerRow % 2;  // Will either be 0 or 1

            iDeadBytes = iBytesPerRow;
            
            if (iTrailingBits > 0) ++iDeadBytes;
            
            iDeadBytes = (QUAD - iDeadBytes % QUAD) % QUAD;

            for (int row = 0; row < bmpInfoHeader_biHeight; ++row) // read over the rows
            {
               if (topDownDIB) i = row; else i = bmpInfoHeader_biHeight - 1 - row;

               for (j = 0; j < iBytesPerRow; ++j)                  // read over bits in row
               {
                  iByteVal = in.readUnsignedByte();

                  for (k = 0; k < 2; ++k) // Two pels per byte
                  {
                     iColumn = j * 2 + k;   
                     
                     /*
                      * 1 - k is needed to have High, Low nibble ordering for the image
                      * and 4 bits are shifted at a time.
                      */
                     pel = colorPallet[(iByteVal >> ((1 - k) * 4)) & 0x0F]; 
                     gPel = colorTo8BitGrayscale(pel);
                     
                     imageArray[i][iColumn] = gPel;
                  }
               } // for (j = 0; j < iBytesPerRow; ++j)

               /*
                * pick up the trailing nibble for images that are not mod 2 columns wide
                */
               if (iTrailingBits > 0) 
               {
                  iByteVal = in.readUnsignedByte();

                  iColumn = iBytesPerRow * 2;
                  
                  /*
                   * The High nibble is the last remaining pel
                   */
                  pel = colorPallet[(iByteVal >> 4) & 0x0F]; 
                  gPel = colorTo8BitGrayscale(pel);
                  
                  imageArray[i][iColumn] = gPel;
               }
               
               /*
                * Now read in the "dead bytes" to pad to a 4 byte boundary
                */
               for (j = 0; j < iDeadBytes; ++j) in.readUnsignedByte(); 
            
            } // for (i = bmpInfoHeader_biHeight - 1; i >= 0; --i)
            
            break;
            
         /*
          * Each byte read in is 1 column. We then read in the dead bytes so that each scan 
          * line is a multiple of 4 bytes.
          */
         case 8: // 1 byte, 1 pel, Works
            iPelsPerRow = bmpInfoHeader_biWidth;
            iDeadBytes = (4 - iPelsPerRow % 4) % 4;
            
            for (int row = 0; row < bmpInfoHeader_biHeight; ++row) // read over the rows
            {
               if (topDownDIB) i = row; else i = bmpInfoHeader_biHeight - 1 - row;

               for (j = 0; j < iPelsPerRow; ++j)                   // j is now just the column counter
               {
                  /*
                   * Need to deal with little endian values
                   */
                  pel = this.swapShort(in.readUnsignedShort());
                  
                  rgbQuad[BLUE]      =  pel        & 0x1F;
                  rgbQuad[GREEN]     = (pel >> 5)  & 0x1F;   
                  rgbQuad[RED]       = (pel >> 10) & 0x1F;
                  
                  pel = (rgbQuad[RED] << 2*BYTE) | (rgbQuad[GREEN] << BYTE) | rgbQuad[BLUE];
                  gPel = colorTo8BitGrayscale(pel);
                  
                  
                  imageArray[i][j] = gPel;
               }

               /*
                * Now read in the "dead bytes" to pad to a 4 byte boundary
                */
               for (j = 0; j < iDeadBytes; ++j) in.readUnsignedByte(); 
               
            } // for (i = bmpInfoHeader_biHeight - 1; i >= 0; --i)
                  
            break; 
         
         /*
          * Each three bytes read in is 1 column. Each scan line is padded to by a 
          * multiple of 4 bytes.
          */
         case 24: // Works
            iPelsPerRow = bmpInfoHeader_biWidth;
            iDeadBytes = (4 - (iPelsPerRow * 3) % 4) % 4;

            for (int row = 0; row < bmpInfoHeader_biHeight; ++row) // read over the rows
            {
               if (topDownDIB) i = row; else i = bmpInfoHeader_biHeight - 1 - row;

               for (j = 0; j < iPelsPerRow; ++j)                   // j is now just the column counter
               {
                  rgbQuad[BLUE]      = in.readUnsignedByte();
                  rgbQuad[GREEN]     = in.readUnsignedByte();
                  rgbQuad[RED]       = in.readUnsignedByte();
                  
                  pel = (rgbQuad[RED] << 2 * BYTE) | (rgbQuad[GREEN] << BYTE) | rgbQuad[BLUE];
                  gPel = colorTo8BitGrayscale(pel);
                  
                  
                  imageArray[i][j] = gPel;
               }
               
               /*
                * Now read in the "dead bytes" to pad to a 4 byte boundary
                */
               for (j = 0; j < iDeadBytes; ++j) in.readUnsignedByte(); 
            
            } // for (int row = 0; row < bmpInfoHeader_biHeight; ++row)
            
            break; 
            
         /*
          * Each four bytes read in is 1 column. The number of bytes per line will always 
          * be a multiple of 4, so there are no dead bytes.
          */
         case 32: // Works
            iPelsPerRow = bmpInfoHeader_biWidth;
            
            for (int row = 0; row < bmpInfoHeader_biHeight; ++row) // read over the rows
            {
               if (topDownDIB) i = row; else i = bmpInfoHeader_biHeight - 1 - row;

               for (j = 0; j < iPelsPerRow; ++j)                   // j is now just the column counter
               {
                  rgbQuad[BLUE]       = in.readUnsignedByte();
                  rgbQuad[GREEN]      = in.readUnsignedByte();
                  rgbQuad[RED]        = in.readUnsignedByte();
                  rgbQuad[RESERVED]   = in.readUnsignedByte();
                  
                  pel =  (rgbQuad[RESERVED] << 3 * BYTE) |(rgbQuad[RED] << 2 * BYTE) | 
                         (rgbQuad[GREEN] << BYTE) | rgbQuad[BLUE];
                         
                  gPel = colorTo8BitGrayscale(pel);
                  
                  imageArray[i][j] = gPel;
               } // for (j = 0; j < iPelsPerRow; ++j) 
               
            } // for (int row = 0; row < bmpInfoHeader_biHeight; ++row)
            
            break; 
         
         default:
            System.out.printf("This error should not occur - 1!\n");
      }  // switch (bmpInfoHeader_biBitCount)
      
      
      shiftImage(findDifferenceCOM(findCOM()));
      
      /*
       * I'm putting iBytesPerRow and iDeadBytes into an array in order to enable multiple outputs.
       */
      outputArray[BYTES_PER_ROW] = iBytesPerRow; 
      outputArray[DEAD_BYTES] = iDeadBytes;
      
      return outputArray;
   }  // public void createArray(DataInputStream fstream)
   
   /* 
    * Prints dump of image bytes in HEX to the console if the image is smaller than 33 x 33
    * 
    * MAX_DIM = 33;
    * 
    * @param iBytesPerRow   the number of bytes per row
    *                       the maximum index for the column counter
    */
   public void printImageBytes(int iBytesPerRow)
   {
      if ((bmpInfoHeader_biWidth < MAX_DIM) && (bmpInfoHeader_biHeight < MAX_DIM))
      {
         iBytesPerRow = bmpInfoHeader_biWidth;
         
         for (int i = 0; i < bmpInfoHeader_biHeight; ++i) // read over the rows
         {
            
            for (int j = 0; j < iBytesPerRow; ++j)        // j is now just the column counter
            {
               System.out.printf("%06X\t", imageArray[i][j]);
            }
            
            System.out.printf("\n");
            
         } // for (int i = 0; i < bmpInfoHeader_biHeight; ++i)
         
      } // if ((bmpInfoHeader_biWidth < MAX_DIM) && (bmpInfoHeader_biHeight < MAX_DIM))
      
      return;
   }
   
   /*
    * This method is mostly here for testing purposes. The method prints the bitmap out to a file.
    * If the BitmapDump class is coded correctly, the bitmap given by the input should be the same 
    * as the bitmap output file produced in this method.
    * 
    * @param iDeadBytes    number of dead bytes per column
    * @param outFileName   name of file with output
    * @param rgbQuad       array of four values indicating intensities of blue, green, and red
    * 
    */
   public void bitmapToFile(int iDeadBytes, String outFileName, int[] rgbQuad) throws IOException
   {
      try
      {
         iDeadBytes = (QUAD - (bmpInfoHeader_biWidth * 3) % QUAD) % QUAD;

         /*bmpInfoHeader_biSizeImage =  (bmpInfoHeader_biWidth * 3 + iDeadBytes) 
                                       * bmpInfoHeader_biHeight;
         bmpFileHeader_bfOffBits = 54;        // 54 byte offset for 24 bit images 
                                              // (just open one with this app to get this value)
         bmpFileHeader_bfSize = bmpInfoHeader_biSizeImage + bmpFileHeader_bfOffBits;
         bmpInfoHeader_biBitCount = 24;       // 24 bit color image
         bmpInfoHeader_biCompression = 0;     // BI_RGB (which is a value of zero)
         bmpInfoHeader_biClrUsed = 0;         // Zero for true color
         bmpInfoHeader_biClrImportant = 0;    // Zero for true color

         FileOutputStream fstream = new FileOutputStream(outFileName);
         DataOutputStream out = new DataOutputStream(fstream);

         // BITMAPFILEHEADER
         out.writeShort(this.swapShort(bmpFileHeader_bfType));      // WORD
         out.writeInt(this.swapInt(bmpFileHeader_bfSize));          // DWORD
         out.writeShort(this.swapShort(bmpFileHeader_bfReserved1)); // WORD
         out.writeShort(this.swapShort(bmpFileHeader_bfReserved2)); // WORD
         out.writeInt(this.swapInt(bmpFileHeader_bfOffBits));       // DWORD

         // BITMAPINFOHEADER
         out.writeInt(this.swapInt(bmpInfoHeader_biSize));          // DWORD
         out.writeInt(this.swapInt(bmpInfoHeader_biWidth));         // LONG
         out.writeInt(this.swapInt(bmpInfoHeader_biHeight));        // LONG
         out.writeShort(this.swapShort(bmpInfoHeader_biPlanes));    // WORD
         out.writeShort(this.swapShort(bmpInfoHeader_biBitCount));  // WORD
         out.writeInt(this.swapInt(bmpInfoHeader_biCompression));   // DWORD
         out.writeInt(this.swapInt(bmpInfoHeader_biSizeImage));     // DWORD
         out.writeInt(this.swapInt(bmpInfoHeader_biXPelsPerMeter)); // LONG
         out.writeInt(this.swapInt(bmpInfoHeader_biYPelsPerMeter)); // LONG
         out.writeInt(this.swapInt(bmpInfoHeader_biClrUsed));       // DWORD
         out.writeInt(this.swapInt(bmpInfoHeader_biClrImportant));  // DWORD
         
         
        
        

         /*
          * There is no color table for this true color image, so the following
          * code writes out the pels.
          */

         /*
          * Write over the rows (i) and the columns (j) in the usual inverted 
          * format
          */
         
         /*for (int i = bmpInfoHeader_biHeight - 1; i >= 0; --i)    
         {
            
            for (int j = 0; j < bmpInfoHeader_biWidth; ++j) 
            {
               int pel = imageArray[i][j];
               
               rgbQuad[BLUE]  = pel; // pel & 0x00FF;
               rgbQuad[GREEN] = pel; // (pel >> BYTE)  & 0x00FF;
               rgbQuad[RED]   = pel; // (pel >> 2*BYTE) & 0x00FF;
               
               out.writeByte(rgbQuad[BLUE]); // lowest byte in the color
               out.writeByte(rgbQuad[GREEN]);
               out.writeByte(rgbQuad[RED]);  // highest byte in the color
            } // for (int j = 0; j < bmpInfoHeader_biWidth; ++j)
            
            for (int j = 0; j < iDeadBytes; ++j)
            {
               
               /*
                * Now write out the "dead bytes" to pad to a 4 byte boundary
                */
               
               /*out.writeByte(0);  
               
            }
            
         } // for (i = bmpInfoHeader_biHeight - 1; i >= 0; --i)
         
         
         /*
          * Prints out lower 8 bits of image array pixel elements
          */
         
         int iBytesPerRow = bmpInfoHeader_biWidth;
         
         PrintWriter out = new PrintWriter(new BufferedWriter(new 
                                        FileWriter(outFileName)));
         
         for (int i = 0; i < bmpInfoHeader_biHeight; ++i) // read over the rows
         {
            
            for (int j = 0; j < iBytesPerRow; ++j)        // j is now just the column counter
            {
               out.printf("%06X\t", imageArray[i][j]);
            }
            
         } // for (int i = 0; i < bmpInfoHeader_biHeight; ++i)
         

         out.close();
         //fstream.close();
      } // try
      
      catch (Exception e)
      {
         System.err.println("File output error" + e);
      }
      
      return;
   } // public void bitmapToFile()

   
}  // public class BitmapDump

/*
 * A member-variable-only class for holding the RGBQUAD C structure elements.
 */
final class RgbQuad
{
   int red;
   int green;
   int blue;
   int reserved;
}
