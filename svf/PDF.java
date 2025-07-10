import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class PDF {
  /**
   * PDF出力
   * ※帳票出力はSVF.javaを利用しています。
   * @param args[0] accessToken 例：77eb1e8fe5f0a0befg24f60c8ef5d4131096fbc4329e2f089f82d30f734f2c9a7a07
   * @param args[1] printerId 例："PDF"もしくは、"Excel"
   * @param args[2] formFilePath 例：form/Test/sample_ja.xml
   * @param args[3] dataFilePath 例：/WebAPISample/sample_ja.csv
   * @param args[4] resourceFilePath 例：/WebAPISample/logo.png
   * @param args[5] downloadFilePath 例：/WebAPI/order.pdf
   */
  public static void main(String[] args) {
    String accessToken = args[0];
    String printerId = args[1];
    String formFilePath = args[2];
    String dataFilePath = args[3];
    String resourceFilePath = args[4];
    String downloadFilePath = args[5];

    // 帳票出力
    String location = SVF.print(accessToken, printerId, formFilePath, dataFilePath, resourceFilePath);
    if (location != null) {
      System.out.println("location=\n" + location);
    } else {
      System.out.println("print error.");
      return;
    }

    // PDFダウンロード
    PDF.download(accessToken, location, downloadFilePath);
    System.out.println("downloadFilePath=\n" + downloadFilePath);

    // 成果物情報の取得
    String artifactInfo = PDF.retrieveAtrifactInfo(accessToken, location);
    System.out.println("artifactInfo=\n" + artifactInfo);

    // 印刷状況の取得
    String printStatusLocation = API_ENDPOINT + ACTIONS_URI + "/" + getActionId(location);
    String printStatus = SVF.retrievePrintStatus(accessToken, printStatusLocation);
    System.out.println("printStatus=\n" + printStatus);
  }

  private static final String API_ENDPOINT = "https://api.svfcloud.com/";// APIを利用するためのドメイン
  private static final String ACTIONS_URI = "v1/actions";// 帳票のを操作するエンドポイント URI

  /**
   * PDFファイルダウンロード
   * @param accessToken
   * @param location
   * @param downloadFilePath
   */
  public static void download(String accessToken, String location, String downloadFilePath) {
    HttpURLConnection conn = null;
    try {
      conn = createGetConnection(location, "application/octet-stream", accessToken);

      int responseCode = conn.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK) {
        // リクエストの結果からファイルを取り出して、出力先ファイルへ書き込みます。
        File outputPath = new File(downloadFilePath);
        BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputPath));
        int len;
        byte[] buf = new byte[8192];
        while ((len = in.read(buf)) != -1) {
          out.write(buf, 0, len);
        }
        out.flush();
        out.close();
        in.close();
      } else if (responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
        location = conn.getHeaderField("Location");
        // 再度呼び出し先が指定された場合には、呼び出しを行います。
        download(accessToken, location, downloadFilePath);
      } else {
        System.out.println(String.format("[%d][%s]", responseCode, conn.getResponseMessage()));
      }
    } catch (MalformedURLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
  }

  /**
   * 成果物情報の取得
   * @param accessToken
   * @param location
   * @return 印刷状況
   */
  private static String retrieveAtrifactInfo(String accessToken, String location) {
    String artifactInfo = null;
    HttpURLConnection conn = null;

    try {
      conn = createGetConnection(location, "application/json", accessToken);

      int responseCode = conn.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK) {
        // リクエストの結果から印刷状況を取り出します。
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
          artifactInfo = reader.readLine();
        }
      } else {
        System.out.println(String.format("[%d][%s]", responseCode, conn.getResponseMessage()));
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
    return artifactInfo;
  }

  /**
   * Getリクエスト用HTTPコネクションの作成
   * @param endPoint
   * @param accessToken
   * @return HttpURLConnection
   * @throws IOException
   */
  private static HttpURLConnection createGetConnection(String endPoint, String accept, String accessToken) throws IOException {
    URL url = null;
    url = new URL(endPoint);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    // Httpリクエストヘッダーの設定
    conn.setRequestProperty("Accept", accept);
    conn.setRequestProperty("Authorization", "Bearer " + accessToken);
    return conn;
  }

  /**
   * PDFダウンロード先のlocation から actionIdの取得
   * @param location
   * @return actionId
   */
  private static String getActionId(String location)
  {
      String[] urls = location.split("\\?");
      String[] params = urls[1].split("&");
      Map<String, String> map = new HashMap<String, String>();
      for (String param : params) {
          String[] split = param.split("=");
          map.put(split[0], split[1]);
      }
      return map.get("action");
  }
}