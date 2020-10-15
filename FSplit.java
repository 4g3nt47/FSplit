package com.umarabdul.util.fsplit;

import java.io.*;
import java.util.*;
import com.umarabdul.util.argparser.ArgParser;

/**
* FSplit is a utility program for splitting a file into various chunks, that can later be
* merged to recreate the original file. Useful when storing a large file in multiple smaller
* storage devices.
*
* @author Umar Abdul
* @version 1.0
* Date: 15/Oct/2020
*/

public class FSplit{

  private int blockSize = 64000; // Bytes to read from files at a time.

  /**
  * Default constructor.
  */
  public FSplit(){

  }

  /**
  * Define the block size to use when reading and writing files.
  * @param blockSize Number of bytes to read at a time.
  */
  public void setBlockSize(int blockSize){
    this.blockSize = (blockSize > 0 ? blockSize : this.blockSize);
  }

  /**
  * Obtain the number of block size in use.
  * @return Number of block size.
  */
  public int getBlockSize(){
    return blockSize;
  }

  /**
  * Split a given file into given number of chunks.
  * You need to have free disk space that is atleast equal to the input file size.
  * @param infile File to split.
  * @param chunks Number of files to create from target file.
  * @param outdir Directory to write chunks to.
  * @throws IOException on IO or logic error.
  */
  public void split(String infile, int chunks, String outdir) throws IOException{

    if (chunks < 2)
      throw new IOException("number of chunks must be greater than 1");
    if (!(outdir.endsWith("/")))
      outdir += "/";
    File file = new File(infile);
    if (!(file.isFile()))
      throw new IOException("invalid input file");
    File dir = new File(outdir);
    if (!(dir.isDirectory())){
      if (!(dir.mkdirs()))
        throw new IOException("error creating output directory: " + outdir);
    }
    long bytesRead = 0;
    int chunksMade = 0;
    long totalSize = file.length();
    long chunkSize = totalSize / chunks;
    if (blockSize > chunkSize)
      throw new IOException(String.format("input file size / number of chunks must be greater than block size (%d)", blockSize));
    byte[] buffer = new byte[blockSize];
    DataOutputStream dos = null;
    DataInputStream dis = new DataInputStream(new FileInputStream(infile));
    System.out.println(String.format("[+] Number of chunks: %d\n[+] Chunk size: %d\n[+] Block size: %d", chunks, chunkSize, blockSize));
    String fname = null;
    int rcount = 0;
    boolean abort = false;
    // Split ;)
    while (chunksMade < chunks){
      System.out.println(String.format("[*] Creating file %d of %d...", chunksMade+1, chunks));
      fname = outdir + String.format("chunk-%03d.fsplit", chunksMade);
      dos = new DataOutputStream(new FileOutputStream(fname));
      bytesRead = 0;
      while (bytesRead < chunkSize){
        rcount = dis.read(buffer, 0, blockSize);
        if (rcount == -1){
          abort = true;
          break;
        }
        dos.write(buffer, 0, rcount);
        bytesRead += rcount;
      }
      chunksMade++;
      if (abort){
        break;
      }
    }
    dos.close();
    dis.close();
    System.out.println("[+] Operation completed!");
    return;
  }

  /**
  * Recreate a file from chunks.
  * You need to have free disk space that is atleast equal to the total size of the chunks.
  * @param dirname Directory containing file chunks.
  * @param outfile Name of file to write merged file to.
  * @throws IOException on IO or logic error.
  */
  public void merge(String dirname, String outfile) throws IOException{

    if (!(dirname.endsWith("/")))
      dirname += "/";
    File dir = new File(dirname);
    if (!(dir.isDirectory()))
      throw new IOException("Invalid directory: " + dirname);
    String[] data = dir.list();
    if (data == null || data.length == 0)
      throw new IOException("empty directory / IO error");
    ArrayList<String> filenames = new ArrayList<String>();
    for (String name : data){
      if (name.startsWith("chunk-") && name.endsWith(".fsplit"))
        filenames.add(name);
    }
    int totalFiles = filenames.size();
    if (totalFiles == 0)
      System.out.println("[-] No file to merge!");
    int count = 0;
    DataOutputStream dos = new DataOutputStream(new FileOutputStream(outfile));
    DataInputStream dis = null;
    byte[] buffer = new byte[blockSize];
    int rcount = 0;
    System.out.println(String.format("[+] Input directory: %s\n[+] Output file: %s\n[+] Block size: %d", dirname, outfile, blockSize));
    // Merge ;)
    while (count < totalFiles){
      System.out.println(String.format("[*] Merging file %d of %d...", count+1, totalFiles));
      dis = new DataInputStream(new FileInputStream(String.format("%schunk-%03d.fsplit", dirname, count)));
      while (true){
        rcount = dis.read(buffer, 0, blockSize);
        if (rcount == -1)
          break;
        dos.write(buffer, 0, rcount);
      }
      dis.close();
      count++;
    }
    dos.close();
    System.out.println("[+] Operation completed!");
    return;
  }

  /**
  * Launch FSplit from command-line.
  * @param args Command-line arguments.
  */
  public static void main(String[] args){

    String helpPage = "FSplit v1.0 - File Splitter and Merger  (Author: github.com/UmarAbdul01)\n"+
                      "      Usage: fsplit [options]\n"+
                      "       Note: Options are interpreted according to mode of operation.\n"+
                      "    Options:\n"+
                      "         -m|--mode       (s)plit/(m)erge     :  Mode of operation\n"+
                      "         -b|--blocksize  <int>               :  Bytes to read at a time\n"+
                      "         -s|--source     <file>/<dir>        :  Input file/directory\n"+
                      "         -d|--dest       <file>/<dir>        :  Destination file / directory\n"+
                      "         -c|--chunks     <int>               :  Number of chunks to create\n"+
                      "         -h|--help                           :  Print this help page";
    ArgParser agp = new ArgParser(args);
    agp.setAlias("mode", "m");
    agp.setAlias("blocksize", "b");
    agp.setAlias("source", "s");
    agp.setAlias("dest", "d");
    agp.setAlias("chunks", "c");
    String mode = agp.getString("mode");
    if (mode == null || (mode.startsWith("s") == false && mode.startsWith("m") == false)|| agp.hasArg("--help") || agp.hasArg("-h")){
      System.out.println(helpPage);
      return;
    }
    FSplit fsplit = new FSplit();
    if (agp.hasKWarg("blocksize"))
      fsplit.setBlockSize(agp.getInt("blocksize"));
    String src = agp.getString("source");
    String dst = agp.getString("dest");
    if (src == null || dst == null){
      System.out.println("[-] --source and --dest arguments required!");
      return;
    }
    if (mode.startsWith("s")){
      if (!(agp.hasKWarg("chunks"))){
        System.out.println("[-] Number of chunks required!");
        return;
      }
      int chunks = agp.getInt("chunks");
      try{
        fsplit.split(src, chunks, dst);
      }catch(IOException e){
        e.printStackTrace();
      }
    }else{
      try{
        fsplit.merge(src, dst);
      }catch(IOException e){
        e.printStackTrace();
      }
    }
    return;
  }
}
