package com.evcharge.libsdk.aliyun;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.evcharge.entity.sys.SysGlobalConfigEntity;

import java.io.File;
import java.net.URL;
import java.util.Date;


public class AliYunOSS {


    public static void uploadFile(String filePath, String objectName) {
        // 创建 OSSClient 对象
        // Endpoint以华东1（杭州）为例，其它Region请按实际情况填写。
        String endpoint = SysGlobalConfigEntity.getString("Aliyun.OSS.File.Endpoint");
        // 填写Bucket名称，例如examplebucket。
        String bucketName = SysGlobalConfigEntity.getString("Aliyun.OSS.File.BucketName");
        String accessKeyId = SysGlobalConfigEntity.getString("Aliyun.OSS.File.AccessKeyId");
        String accessKeySecret = SysGlobalConfigEntity.getString("Aliyun.OSS.File.AccessKeySecret");
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            // 创建PutObjectRequest对象。
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, new File(filePath));
            // 如果需要上传时设置存储类型和访问权限，请参考以下示例代码。
            // ObjectMetadata metadata = new ObjectMetadata();
            // metadata.setHeader(OSSHeaders.OSS_STORAGE_CLASS, StorageClass.Standard.toString());
            // metadata.setObjectAcl(CannedAccessControlList.Private);
            // putObjectRequest.setMetadata(metadata);
            // 上传文件。
            PutObjectResult result = ossClient.putObject(putObjectRequest);
            System.out.println(result);
            /**
             * TOOD 需要做判断文件是否上传成功
             */
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 获取图片链接
     *
     * @param fileName
     * @return
     */
    public static String getImageUrl(String fileName) {
//        String endpoint = SysGlobalConfigEntity.getString("Aliyun.OSS.File.Endpoint");
        return getImageUrl(fileName, 3600l * 1000 * 24 * 365 * 10);
    }

    public static String getImageUrl(String fileName, long expirationTime) {
//        String endpoint = SysGlobalConfigEntity.getString("Aliyun.OSS.File.Endpoint");
        String bucketName = SysGlobalConfigEntity.getString("Aliyun.OSS.File.BucketName");
//        //文件访问路径
//        // 关闭ossClient
//        return endpoint.split("//")[0] + "//" + bucketName + "." + endpoint.split("//")[1] + "/" + fileName;// 把上传到oss的路径返回

        // 设置URL过期时间为10年  3600l* 1000*24*365*10
        Date expiration = new Date(new Date().getTime() + expirationTime);
        // 生成URL
        String accessKeyId = SysGlobalConfigEntity.getString("Aliyun.OSS.File.AccessKeyId");
        String accessKeySecret = SysGlobalConfigEntity.getString("Aliyun.OSS.File.AccessKeySecret");
        String endpoint = SysGlobalConfigEntity.getString("Aliyun.OSS.File.Endpoint");
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        URL url = ossClient.generatePresignedUrl(bucketName, fileName, expiration);
        if (url != null) {
            return url.toString();
        }
        return null;
    }


    public void del() {

    }


}
