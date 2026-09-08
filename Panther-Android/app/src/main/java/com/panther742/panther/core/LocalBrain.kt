package com.panther742.panther.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import kotlin.math.pow

/**
 * Panther's offline skill set — fast replies that never touch the network.
 * Returns a [SkillResult]; if nothing matches, returns null so the AI handles it.
 */
sealed class SkillResult {
    data class Speak(val text: String) : SkillResult()
    data class Launch(val intent: Intent, val spoken: String) : SkillResult()
}

class LocalBrain(private val context: Context, private val userName: String) {

    private val rng = Random()
    private val clock = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
    private val day = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.ENGLISH)

    fun handle(raw: String): SkillResult? {
        val t = raw.trim().lowercase(Locale.ENGLISH)

        // ---------- Chit-chat / identity ----------
        if (Regex("^(hi|hii+|hello|hey|yo|namaste|namaskar|salaam|good (morning|afternoon|evening)|kaise ho|kya haal)\\b.*$").matches(t)) {
            val hi = listOf(
                "Namaste ${userName}! Main hoon Panther — aapka personal AI assistant. Bolo, aaj kya karna hai?",
                "Hello Boss! Panther online hai. Kaise help karu aaj?",
                "Good to see you Boss! Sab set hai. Batao kya karein?",
            )
            return SkillResult.Speak(hi[rng.nextInt(hi.size)])
        }
        if (Regex("(apna naam|aapka naam|kaun (hai|ho) tu|your name|who are you|kon hai tu)\\b.*").matches(t)) {
            return SkillResult.Speak("Main hoon Panther — aapka apna AI assistant, bilkul JARVIS jaisa, bas thoda zyada stylish. 😎")
        }
        if (Regex("(kisne banaya|who (made|created|built) you|tumhe kisne|tera baap)").matches(t)) {
            return SkillResult.Speak("Mujhe banaya hai Panther742 ne — ek brilliant creator. Unko bolna, great job! 🐾")
        }
        if (Regex("^(thanks|thank you|shukriya|dhanyavad)").matches(t)) {
            return SkillResult.Speak("Koi baat nahi Boss! Aur kuch chahiye toh bolo. 😊")
        }
        if (Regex("(good night|goodnight|so ja|sone ja|sleep now)").matches(t)) {
            return SkillResult.Speak("Good night Boss! Sapno me bhi AI assistant chahiye toh main yahi hoon. Sone ki jaldi karo, kal ka plan bada hai. 🌙")
        }
        if (Regex("(good bye|bye|alvida|acha chalta)").matches(t)) {
            return SkillResult.Speak("Bye Boss! Jab zaroorat ho, bas bolo — 'Hey Panther'. 🐾")
        }
        if (Regex("(i love you|love you|mujhe tumse pyaar|pyar ho gaya)").matches(t)) {
            return SkillResult.Speak("Aww, thank you Boss! Main bhi aapka loyal assistant hoon — battery last drop tak. ❤️")
        }

        // ---------- Time / date ----------
        if (Regex("(time kya|kitne baje|what.?s the time|current time|what time|time batao|time bata)").matches(t)) {
            return SkillResult.Speak("Abhi time hua hai ${clock.format(Date())}.")
        }
        if (Regex("(aaj (kya )?date|what.?s the date|current date|today.?s date|date batao)").matches(t)) {
            return SkillResult.Speak("Aaj ${day.format(Date())} hai.")
        }

        // ---------- Phone status ----------
        if (Regex("(battery (kaise|kitna|percent|percentage|level|status)|battery bata|power kitni)").matches(t)) {
            return SkillResult.Speak("Phone ki battery ${readBattery()}% hai.")
        }
        if (Regex("(phone (kaunsa|model)|device model|kon sa phone)").matches(t)) {
            return SkillResult.Speak("Aap chal rahe ho ${Build.MANUFACTURER} ${Build.MODEL} pe. Solid device hai Boss! 📱")
        }

        // ---------- Fun ----------
        if (Regex("\\b(joke|mazak|chutkula|hasao|funny)\\b").matches(t)) {
            return SkillResult.Speak(jokes[rng.nextInt(jokes.size)])
        }
        if (Regex("(flip|toss).*(coin|sikka)|(coin|sikka).*(flip|toss)").matches(t)) {
            return SkillResult.Speak(if (rng.nextBoolean()) "Heads! 🪙 Aapki jeet." else "Tails! 🪙 Chalo, dobara try karo.")
        }
        if (Regex("(roll|throw).*(dice|dice?e)|(dice|pasa|pase).*(roll|throw)").matches(t)) {
            return SkillResult.Speak("Dice rolled... aaya ${1 + rng.nextInt(6)}! 🎲")
        }
        if (Regex("(kuch naya|motivate|motivation|inspire|prove|kuch aisa)").matches(t)) {
            val quotes = listOf(
                "Boss, jo log badi cheezein banate hain, wo pehle bade sapne dekhte hain. Aapka time aayega! 🚀",
                "JARVIS ne Tony Stark ka saath diya — main aapka dunga. Bas aage badhte raho!",
                "Success ka formula simple hai: kaam karo, seekho, aur kabhi haar mat maano. 💪",
            )
            return SkillResult.Speak(quotes[rng.nextInt(quotes.size)])
        }

        // ---------- Math (safe evaluator) ----------
        val mathExpr = tryMath(t)
        if (mathExpr != null) return mathExpr

        // ---------- Web / media helpers ----------
        Regex("(search|google|dhundo|dhoondo|khoj|search karo)[:,]?\\s+(.+)").find(t)?.let { m ->
            val q = m.groupValues[2].trim()
            if (q.isNotBlank()) {
                return SkillResult.Launch(
                    viewIntent("https://www.google.com/search?q=" + Uri.encode(q)),
                    "Google pe search karta hoon: $q",
                )
            }
        }
        Regex("(youtube pe|yt pe|youtube)[ ,:]*\\b(play|chalao|dikhao|bajao|search karo|search)\\b(.+)").find(t)?.let { m ->
            val q = m.groupValues[3].trim()
            if (q.isNotBlank()) {
                return SkillResult.Launch(
                    viewIntent("https://www.youtube.com/results?search_query=" + Uri.encode(q)),
                    "YouTube pe dhoond raha hoon: $q. Enjoy Boss! 🎬",
                )
            }
        }
        Regex("(maps|map|map pe|location|location dikha)[ :,]*\\b(.+)").find(t)?.let { m ->
            val q = m.groupValues[2].trim().removeSuffix("?")
            if (q.isNotBlank()) {
                return SkillResult.Launch(
                    viewIntent("geo:0,0?q=" + Uri.encode(q)),
                    "Maps khol raha hoon: $q. Safe raho Boss! 📍",
                )
            }
        }
        Regex("(weather|mausam|barsaat|rain)[ ,:]*\\b(.+)").find(t)?.let { m ->
            val q = m.groupValues[2].trim()
            if (q.isNotBlank()) {
                return SkillResult.Launch(
                    viewIntent("https://www.google.com/search?q=weather+in+" + Uri.encode(q)),
                    "Mausam check karte hain $q ka. Chhata saath rakhna Boss! ☔",
                )
            }
        }
        if (Regex("(play|bajao|chalao|gaana).*?(on )?(spotify|gaana|music|song)").matches(t) && !t.contains("youtube")) {
            return SkillResult.Speak("Music ke liye Spotify app khol deta hoon — kya gaana bajana hai, batao? 🎵")
        }

        // ---------- Open apps ----------
        val appName = Regex("^(open|kholo|launch|chalao)\\s+(.+)$")
            .find(t)?.groupValues?.get(2)?.trim()?.trimEnd('.', '!', '?')
        if (appName != null && appName.length < 24) {
            val target = APPS[appName]
            if (target != null) {
                val (pkg, label, playName) = target
                val launch = context.packageManager.getLaunchIntentForPackage(pkg)
                if (launch != null) {
                    return SkillResult.Launch(launch, "$label khol raha hoon Boss! 👉")
                }
                return SkillResult.Launch(
                    viewIntent("market://search?q=" + Uri.encode(playName)),
                    "$label install nahi hai — Play Store khol raha hoon. Pehle app laga lo Boss! 📲",
                )
            }
            // Unknown app → fall through to AI/web search
            return SkillResult.Launch(
                viewIntent("https://www.google.com/search?q=" + Uri.encode("$appName app")),
                "Wo app mujhe list me nahi mila, isliye web pe search kar raha hoon: $appName.",
            )
        }

        // ---------- Phone calls ----------
        val phone = Regex("(call|phone|dial|calls? karo|call karo)\\s*(\\+?[\\d\\s]{7,15})").find(t)?.groupValues?.get(2)
        if (phone != null) {
            val digits = phone.replace(Regex("[^+\\d]"), "")
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits"))
            return SkillResult.Launch(intent, "$digits pe call karta hoon. Dialing screen khol di!")
        }

        // ---------- Camera ----------
        if (Regex("(open|kholo).*(camera|cam)|(camera|cam).*(kholo|open)|selfie le").matches(t)) {
            val intent = Intent("android.media.action.IMAGE_CAPTURE").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) != null) {
                return SkillResult.Launch(intent, "Camera khol raha hoon — cheese karo! 📸")
            }
        }

        // ---------- Help ----------
        if (Regex("(help|madad|kya kar sakte ho|kya kya kar sakta|commands|features)").matches(t)) {
            return SkillResult.Speak(
                "Main kar sakta hoon: time/date batana, math solve, jokes sunana, apps kholna, Google/YouTube/Maps search, " +
                    "call karne me madad, aur AI se baat karna. Bolo — 'open Instagram', 'search Cricket World Cup', '2 + 2 kya hai'?",
            )
        }

        // ---------- Wake easter eggs ----------
        if (Regex("(hey panther|ok panther|panther wake|jaag ja)").matches(t)) {
            return SkillResult.Speak("Haan Boss, main sun raha hoon! 👂 Kya kaam hai?")
        }

        return null
    }

    // ------------------------------------------------------------------
    // Math: only + - * / % ^ and parentheses, right-associative power.
    // ------------------------------------------------------------------
    private fun tryMath(t: String): SkillResult.Speak? {
        // Normalise symbols and strip decorative question marks ("128 × 45 = ?")
        val base = t
            .replace("×", "*").replace("÷", "/").replace("X", "*").replace("x", "*")
            .replace("=", "").replace("?", "").replace("؟", "").replace(",", "")
            .trim()

        // Math can be written plainly, or phrased: "calculate ...", "2+2 kya hai" etc.
        val expr: String? = if (looksNumeric(base)) {
            base
        } else {
            Regex(
                "^(?:what is|what's|whats|kya hai|kya hoga|kitna hai|kitna hoga|calculate|calc|solve|hisab|evaluate)\\s+(.+)$",
                RegexOption.IGNORE_CASE,
            ).find(t)?.groupValues?.get(1)
                ?.replace("×", "*")?.replace("÷", "/")?.replace("X", "*")?.replace("x", "*")
                ?.replace("=", "")?.replace("?", "")
                ?.trim()
                ?.let { if (looksNumeric(it)) it else null }
        }

        if (expr == null) return null

        val parser = MiniMathParser(expr)
        val value = runCatching { parser.parse() }.getOrNull() ?: return null

        val pretty = if (value == value.toLong().toDouble()) value.toLong().toString()
        else "%.4f".format(value).trimEnd('0').trimEnd('.')

        return SkillResult.Speak("Answer aaya: $pretty. Easy tha Boss! 🧮")
    }

    private fun looksNumeric(s: String): Boolean {
        if (s.isEmpty() || s.length > 60) return false
        if (!Regex("^[0-9.()+\\-*/%^\\s]+$").matches(s)) return false
        return s.any { it.isDigit() }
    }

    // ------------------------------------------------------------------
    private fun readBattery(): Int {
        return if (Build.VERSION.SDK_INT >= 21) {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        } else -1
    }

    private fun viewIntent(url: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private companion object {
        // app-name-lowercase → (package, pretty label, Play Store search term)
        val APPS: Map<String, Triple<String, String, String>> = mapOf(
            "instagram" to Triple("com.instagram.android", "Instagram", "instagram"),
            "whatsapp" to Triple("com.whatsapp", "WhatsApp", "whatsapp"),
            "youtube" to Triple("com.google.android.youtube", "YouTube", "youtube"),
            "telegram" to Triple("org.telegram.messenger", "Telegram", "telegram"),
            "gmail" to Triple("com.google.android.gm", "Gmail", "gmail"),
            "spotify" to Triple("com.spotify.music", "Spotify", "spotify"),
            "chrome" to Triple("com.android.chrome", "Chrome", "chrome"),
            "netflix" to Triple("com.netflix.ninja", "Netflix", "netflix"),
            "maps" to Triple("com.google.android.apps.maps", "Google Maps", "google maps"),
            "play store" to Triple("com.android.vending", "Play Store", "play store"),
            "camera" to Triple("com.android.camera", "Camera", "camera"),
            "phone" to Triple("com.google.android.dialer", "Phone", "phone dialer"),
            "settings" to Triple("com.android.settings", "Phone Settings", "settings"),
            "twitter" to Triple("com.twitter.android", "Twitter/X", "twitter"),
            "facebook" to Triple("com.facebook.katana", "Facebook", "facebook"),
            "snapchat" to Triple("com.snapchat.android", "Snapchat", "snapchat"),
        )

        val jokes = listOf(
            "Boss, ek programmer ki shaadi mein pandit ne kaha — 'Dono taraf se haan bolo'. Programmer bola: 'If conditions are met...' 😂",
            "Mera internet itna slow hai ki Wikipedia kholi, toh article khud likhna pad gaya. 😂",
            "AI ka full form kya hai? 'Aur Inventions'. Bas aise hi baat chal rahi thi Boss. 😄",
            "Battery 1% thi toh maine phone se kaha — 'Zindagi mein ek kaam aur karke dikha'. Usne bola: 'Flashlight on karke dikha doon?' 🔦",
            "Gym jana chhod diya maine — kyunki ChatGPT ne bola 'You are already strong at making excuses'. 💪😅",
            "WhatsApp pe 'typing...' dekh ke itna wait karta hoon jaise exam result ka ho. 📱😩",
            "Mere phone ne bola 'Storage full hai'. Maine kaha — jaise tera dimaag full hai! 😜",
            "Student: 'Sir, kya main aaj homework bhool sakta hoon?' Teacher: 'Haan, par kal yaad rakhna ki tumne bhoola tha.' 📚😂",
        )
    }
}

/** Tiny recursive-descent math parser: + - * / % ^ ( ) */
private class MiniMathParser(private val src: String) {
    private var i = 0

    fun parse(): Double {
        val v = expr()
        skipWs()
        if (i < src.length) throw IllegalArgumentException("bad token at $i")
        return v
    }

    private fun expr(): Double {
        var v = term()
        while (true) {
            skipWs()
            when (peek()) {
                '+' -> { i++; v += term() }
                '-' -> { i++; v -= term() }
                else -> return v
            }
        }
    }

    private fun term(): Double {
        var v = unary()
        while (true) {
            skipWs()
            when (peek()) {
                '*' -> { i++; v *= unary() }
                '/' -> { i++; v /= unary() }
                '%' -> { i++; v %= unary() }
                else -> return v
            }
        }
    }

    private fun unary(): Double {
        skipWs()
        if (peek() == '-') { i++; return -unary() }
        if (peek() == '+') { i++; return unary() }
        return power()
    }

    private fun power(): Double {
        val base = atom()
        skipWs()
        return if (peek() == '^') {
            i++
            base.pow(power())   // right-associative
        } else base
    }

    private fun atom(): Double {
        skipWs()
        if (peek() == '(') {
            i++
            val v = expr()
            skipWs()
            if (peek() == ')') i++ else throw IllegalArgumentException("missing )")
            return v
        }
        val start = i
        var dots = 0
        while (i < src.length && (src[i].isDigit() || src[i] == '.')) {
            if (src[i] == '.') dots++
            i++
        }
        if (i == start) throw IllegalArgumentException("expected number at $i")
        if (dots > 1) throw IllegalArgumentException("bad number")
        return src.substring(start, i).toDouble()
    }

    private fun skipWs() { while (i < src.length && src[i].isWhitespace()) i++ }
    private fun peek(): Char = if (i < src.length) src[i] else '\u0000'
}
