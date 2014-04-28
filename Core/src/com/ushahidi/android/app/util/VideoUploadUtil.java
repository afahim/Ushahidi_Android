/*
 * Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ushahidi.android.app.util;

import android.annotation.SuppressLint;
import android.util.Log;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.InputStreamContent;
import com.ushahidi.android.app.util.YouTubeAuth;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.scribe.oauth.OAuthService;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.VimeoApi;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.*;


/**
 * Upload a video to the authenticated user's channel. Use OAuth 2.0 to
 * authorize the request. Note that you must add your video files to the
 * project folder to upload them with this application.
 *
 * @author Jeremy Walker
 */
public class VideoUploadUtil {

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private static YouTube youtube;

    /**
     * Define a global variable that specifies the MIME type of the video
     * being uploaded.
     */
    private static final String VIDEO_FILE_FORMAT = "video/*";

    private static final String SAMPLE_VIDEO_FILENAME = "sample-video.mp4";

    private static OAuthService service;
    private static Token accessToken;
    private static String fileLocation;
    private static String newline = System.getProperty("line.separator");
    private static int bufferSize = 1048576; // 1 MB = 1048576 bytes
    
    public static void uploadVimeoVideo(String fLocation) {
    	fileLocation = fLocation;
    	
        String apiKey = "46808ac7e90549f2536ab035b449b095bc2fa373"; //Give your own API Key / Client ID / Consumer Key
        String apiSecret = "98bbb44fb4fbcf5d8a32a051a7e4d5289a4b9470"; //Give your own API Secret / Client Secret / Consumer Secret
        String vimeoAPIURL = "http://vimeo.com/api/rest/v2";
        service = new ServiceBuilder().provider(VimeoApi.class).apiKey(apiKey).apiSecret(apiSecret).build();

        OAuthRequest request;
        Response response;
     
        accessToken = new Token("your_token", "your_token's_secret"); //Copy the new token you get here
        
        accessToken = checkToken(vimeoAPIURL, accessToken, service);
        if (accessToken == null) {
          return;
        }

        
    }
    
    /**
     * Checks the token to make sure it's still valid. If not, it pops up a dialog asking the user to
     * authenticate.
     */
     private static Token checkToken(String vimeoAPIURL, Token vimeoToken, OAuthService vimeoService) {
       if (vimeoToken == null) {
         vimeoToken = getNewToken(vimeoService);
       } else {
         OAuthRequest request = new OAuthRequest(Verb.GET, vimeoAPIURL);
         request.addQuerystringParameter("method", "vimeo.oauth.checkAccessToken");
         Response response = signAndSendToVimeo(request, "checkAccessToken", true);
         if (response.isSuccessful()
                 && (response.getCode() != 200 || response.getBody().contains("<err code=\"302\"")
                 || response.getBody().contains("<err code=\"401\""))) {
           vimeoToken = getNewToken(vimeoService);
         }
       }
       return vimeoToken;
     }
     
     /**
      * Signs the request and sends it. Returns the response.
      *
      * @param request
      * @return response
      */
	@SuppressLint("NewApi")
	public static Response signAndSendToVimeo(OAuthRequest request, String description, boolean printBody) throws org.scribe.exceptions.OAuthException {
        System.out.println(newline + newline
                + "Signing " + description + " request:"
                + ((printBody && !request.getBodyContents().isEmpty()) ? newline + "\tBody Contents:" + request.getBodyContents() : "")
                + ((!request.getHeaders().isEmpty()) ? newline + "\tHeaders: " + request.getHeaders() : ""));
        service.signRequest(accessToken, request);
        printRequest(request, description);
        Response response = request.send();
        printResponse(response, description, printBody);
        return response;
      }

     /**
      * Gets authorization URL, pops up a dialog asking the user to authenticate with the url and the user
      * returns the authorization code
      *
      * @param service
      * @return
      */
      private static Token getNewToken(OAuthService service) {
        // Obtain the Authorization URL
        Token requestToken = service.getRequestToken();
        String authorizationUrl = service.getAuthorizationUrl(requestToken);
        do {
          Log.v("VidGrab", "Auth URL: " + authorizationUrl);
          break;
          /*code = null;
          Verifier verifier = new Verifier(code);
          // Trade the Request Token and Verfier for the Access Token
          System.out.println("Trading the Request Token for an Access Token...");
          try {
            Token token = service.getAccessToken(requestToken, verifier);
            System.out.println(token); //Use this output to copy the token into your code so you don't have to do this over and over.
            return token;
          } catch (OAuthException ex) {
            int choice = JOptionPane.showConfirmDialog(null, "There was an OAuthException" + newline
                    + ex + newline
                    + "Would you like to try again?", "OAuthException", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.NO_OPTION) {
              break;
            }
          }*/
        } while (true);
        return null;
      }

      /**
       * Prints the given description, and the headers, verb, and complete URL of the request.
       *
       * @param request
       * @param description
       */
      private static void printRequest(OAuthRequest request, String description) {
        Log.v("Req", "");
        Log.v("Req", description + " >>> Request");
        Log.v("Req", "Headers: " + request.getHeaders());
        Log.v("Req", "Verb: " + request.getVerb());
        Log.v("Req", "Complete URL: " + request.getCompleteUrl());
      }

      /**
       * Prints the given description, and the code, headers, and body of the given response
       *
       * @param response
       * @param description
       */
      private static void printResponse(Response response, String description, boolean printBody) {
        Log.v("Req", "");
        Log.v("Req", description + " >>> Response");
        Log.v("Req", "Code: " + response.getCode());
        Log.v("Req", "Headers: " + response.getHeaders());
        if (printBody) {
          Log.v("Req", "Body: " + response.getBody());
        }
      }

      
      
    /**
     * Upload the user-selected video to the user's YouTube channel. The code
     * looks for the video in the application's project folder and uses OAuth
     * 2.0 to authorize the API request.
     *
     * @param args command line args (not used).
     */
    public static void uploadYoutubeVideo(String fileLocation) {

        // This OAuth 2.0 access scope allows an application to upload files
        // to the authenticated user's YouTube channel, but doesn't allow
        // other types of access.
        List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube.upload");

        try {
            // Authorize the request.
            Credential credential = YouTubeAuth.authorize(scopes, "uploadvideo");

            // This object is used to make YouTube Data API requests.
            youtube = new YouTube.Builder(YouTubeAuth.HTTP_TRANSPORT, YouTubeAuth.JSON_FACTORY, credential).setApplicationName(
                    "youtube-cmdline-uploadvideo-sample").build();

            System.out.println("Uploading: " + SAMPLE_VIDEO_FILENAME);

            // Add extra information to the video before uploading.
            Video videoObjectDefiningMetadata = new Video();

            // Set the video to be publicly visible. This is the default
            // setting. Other supporting settings are "unlisted" and "private."
            VideoStatus status = new VideoStatus();
            status.setPrivacyStatus("public");
            videoObjectDefiningMetadata.setStatus(status);

            // Most of the video's metadata is set on the VideoSnippet object.
            VideoSnippet snippet = new VideoSnippet();

            // This code uses a Calendar instance to create a unique name and
            // description for test purposes so that you can easily upload
            // multiple files. You should remove this code from your project
            // and use your own standard names instead.
            Calendar cal = Calendar.getInstance();
            snippet.setTitle("Test Upload via Java on " + cal.getTime());
            snippet.setDescription(
                    "Video uploaded via YouTube Data API V3 using the Java library " + "on " + cal.getTime());

            // Set the keyword tags that you want to associate with the video.
            List<String> tags = new ArrayList<String>();
            tags.add("test");
            tags.add("example");
            tags.add("java");
            tags.add("YouTube Data API V3");
            tags.add("erase me");
            snippet.setTags(tags);

            // Add the completed snippet object to the video resource.
            videoObjectDefiningMetadata.setSnippet(snippet);

            InputStreamContent mediaContent = new InputStreamContent(VIDEO_FILE_FORMAT,
                    VideoUploadUtil.class.getResourceAsStream(fileLocation));

            // Insert the video. The command sends three arguments. The first
            // specifies which information the API request is setting and which
            // information the API response should return. The second argument
            // is the video resource that contains metadata about the new video.
            // The third argument is the actual video content.
            YouTube.Videos.Insert videoInsert = youtube.videos()
                    .insert("snippet,statistics,status", videoObjectDefiningMetadata, mediaContent);

            // Set the upload type and add an event listener.
            MediaHttpUploader uploader = videoInsert.getMediaHttpUploader();

            // Indicate whether direct media upload is enabled. A value of
            // "True" indicates that direct media upload is enabled and that
            // the entire media content will be uploaded in a single request.
            // A value of "False," which is the default, indicates that the
            // request will use the resumable media upload protocol, which
            // supports the ability to resume an upload operation after a
            // network interruption or other transmission failure, saving
            // time and bandwidth in the event of network failures.
            uploader.setDirectUploadEnabled(false);

            MediaHttpUploaderProgressListener progressListener = new MediaHttpUploaderProgressListener() {
                public void progressChanged(MediaHttpUploader uploader) throws IOException {
                    switch (uploader.getUploadState()) {
                        case INITIATION_STARTED:
                            System.out.println("Initiation Started");
                            break;
                        case INITIATION_COMPLETE:
                            System.out.println("Initiation Completed");
                            break;
                        case MEDIA_IN_PROGRESS:
                            System.out.println("Upload in progress");
                            System.out.println("Upload percentage: " + uploader.getProgress());
                            break;
                        case MEDIA_COMPLETE:
                            System.out.println("Upload Completed!");
                            break;
                        case NOT_STARTED:
                            System.out.println("Upload Not Started!");
                            break;
                    }
                }
            };
            uploader.setProgressListener(progressListener);

            // Call the API and upload the video.
            Video returnedVideo = videoInsert.execute();

            // Print data about the newly inserted video from the API response.
            System.out.println("\n================== Returned Video ==================\n");
            System.out.println("  - Id: " + returnedVideo.getId());
            System.out.println("  - Title: " + returnedVideo.getSnippet().getTitle());
            System.out.println("  - Tags: " + returnedVideo.getSnippet().getTags());
            System.out.println("  - Privacy Status: " + returnedVideo.getStatus().getPrivacyStatus());
            System.out.println("  - Video Count: " + returnedVideo.getStatistics().getViewCount());

        } catch (GoogleJsonResponseException e) {
            System.err.println("GoogleJsonResponseException code: " + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            e.printStackTrace();
        } catch (Throwable t) {
            System.err.println("Throwable: " + t.getMessage());
            t.printStackTrace();
        }
    }
}
