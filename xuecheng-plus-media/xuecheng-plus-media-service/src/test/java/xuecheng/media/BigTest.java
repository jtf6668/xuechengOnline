package xuecheng.media;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 测试大文件上传
 */
public class BigTest {
    @Test
    public void testChunk() throws Exception {
        //源文件，被上传的文件
        File sourseFile = new File("D:\\develop\\upload\\1.mp4");
        //分块文件路径，文件上传后放到哪里
        String chunkFilePath = "D:\\develop\\upload\\chunk\\";
        //分块文件大小
        int chunkSize = 1024 * 1024 *1;//1M
        //分块文件个数
        int chunkNumber = (int) Math.ceil(sourseFile.length() * 1.0 / chunkSize);//*1.0的目的是能得出小数，ceil是向上取整
        //使用随机流从源文件读数据
        RandomAccessFile raf_r = new RandomAccessFile(sourseFile, "r");//r代表读数据
        //缓冲区,把数据先读到缓冲区
        byte[] bytes = new byte[1024];
        //
        for (int i = 0; i < chunkNumber; i++) {
            File chunkFile = new File(chunkFilePath + i);//分块文件
            //分块文件的写入流
            RandomAccessFile raf_rw= new RandomAccessFile(chunkFile, "rw");
            int len = -1;//
            while ((len = raf_r.read(bytes))!=-1){//每次读bytes大小的文件，读到最后一行就停止
                raf_rw.write(bytes,0,len);//写入读到的文件
                if(chunkFile.length()>=chunkSize){//如果读取的内容已经达到了文件的最大容量，也退出循环
                    break;
                }

            }
            raf_rw.close();//写入的流在循环里创建。每次循环读完，写文件的流就可以关闭了
        }
        raf_r.close();//全部弄完，写文件的流也可以关闭了
    }


    @Test
    public void testMerge() throws IOException {
        //块文件目录
        File chunkFolder = new File("D:\\develop\\upload\\chunk");
        //源文件
        File sourceFile = new File("D:\\develop\\upload\\1.mp4");
        //合并后的文件
        File mergeFile = new File("D:\\develop\\upload\\1.项目背景_2.mp4");

        //取出所有分块文件
        File[] files = chunkFolder.listFiles();
        //将数组转成list
        List<File> filesList = Arrays.asList(files);
        //对分块文件排序
        Collections.sort(filesList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Integer.parseInt(o1.getName())-Integer.parseInt(o2.getName());
            }
        });
        //向合并文件写的流
        RandomAccessFile raf_rw = new RandomAccessFile(mergeFile, "rw");
        //缓存区
        byte[] bytes = new byte[1024];
        //遍历分块文件，向合并 的文件写
        for (File file : filesList) {
            //读分块的流
            RandomAccessFile raf_r = new RandomAccessFile(file, "r");
            int len = -1;
            while ((len=raf_r.read(bytes))!=-1){
                raf_rw.write(bytes,0,len);
            }
            raf_r.close();

        }
        raf_rw.close();
        //合并文件完成后对合并的文件md5校验
        FileInputStream fileInputStream_merge = new FileInputStream(mergeFile);
        FileInputStream fileInputStream_source = new FileInputStream(sourceFile);
        String md5_merge = DigestUtils.md5Hex(fileInputStream_merge);
        String md5_source = DigestUtils.md5Hex(fileInputStream_source);
        if(md5_merge.equals(md5_source)){
            System.out.println("文件合并成功");
        }

    }
}
