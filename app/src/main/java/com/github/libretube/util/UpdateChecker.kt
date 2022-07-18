package com.github.libretube.util

import android.util.Log
import com.github.libretube.GITHUB_API_URL
import com.github.libretube.obj.VersionInfo
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection

fun checkUpdate(): VersionInfo? {
    var versionInfo: VersionInfo? = VersionInfo("", "")
    // run http request as thread to make it async
    val thread = Thread {
        // otherwise crashes without internet
        try {
            versionInfo = getUpdateInfo()
        } catch (e: Exception) {
        }
    }
    thread.start()
    // wait for the thread to finish
    thread.join()

    // return the information about the latest version
    return versionInfo
}

fun getUpdateInfo(): VersionInfo? {
    val latest = URL(GITHUB_API_URL)
    val json = StringBuilder()
    val urlConnection: HttpsURLConnection?
    urlConnection = latest.openConnection() as HttpsURLConnection
    val br = BufferedReader(InputStreamReader(urlConnection.inputStream))

    var line: String?
    while (br.readLine().also { line = it } != null) json.append(line)

    // Parse and return the json data
    val jsonRoot = JSONObject(json.toString())
    if (jsonRoot.has("tag_name") &&
        jsonRoot.has("html_url") &&
        jsonRoot.has("assets")
    ) {
        val updateUrl = jsonRoot.getString("html_url")
        val jsonAssets: JSONArray = jsonRoot.getJSONArray("assets")

        for (i in 0 until jsonAssets.length()) {
            val jsonAsset = jsonAssets.getJSONObject(i)
            if (jsonAsset.has("name")) {
                val name = jsonAsset.getString("name")
                if (name.endsWith(".apk")) {
                    val tagName = jsonRoot.getString("name")
                    Log.i("VersionInfo", "Latest version: $tagName")
                    return VersionInfo(updateUrl, tagName)
                }
            }
        }
    }
    return null
}
