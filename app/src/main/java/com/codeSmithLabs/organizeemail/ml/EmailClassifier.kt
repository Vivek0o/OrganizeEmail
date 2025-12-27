package com.codeSmithLabs.organizeemail.ml


class EmailClassifier {

    companion object {
        const val FINANCE = "Finance"
        const val JOBS = "Jobs"
        const val SHOPPING = "Shopping"
        const val TRAVEL = "Travel"
        const val SOCIAL = "Social"
        const val TECH = "Tech"
        const val ENTERTAINMENT = "Entertainment"
        const val EDUCATION = "Education"
        const val PROMOTIONS = "Promotions"
        const val OTHER = "Other"
    }

    /**
     * Layer 1: Strong Intent Patterns
     * These patterns have extremely high precision. If matched in Subject or Snippet,
     * they immediately determine the category, ignoring the sender.
     */
    private val strongIntentRules = mapOf(
        JOBS to listOf(
            "interview schedule", "job offer", "application received", "hiring manager",
            "talent acquisition", "your candidature", "job application", "apply for",
            "referral", "position at", "offer letter", "joining date", "employment",
            "careers team", "recruiting", "shortlisted"
        ),
        FINANCE to listOf(
            "one time password", "otp is", "payment successful", "transaction alert",
            "credit card statement", "amount due", "payment received", "money sent",
            "fund transfer", "bank statement", "account balance", "payment scheduled",
            "withdrawal", "deposited"
        ),
        TRAVEL to listOf(
            "pnr", "boarding pass", "e-ticket", "flight confirmation", "hotel reservation",
            "booking id", "web check-in", "trip itinerary", "journey details"
        ),
        SHOPPING to listOf(
            "order placed", "order confirmation", "out for delivery", "package delivered",
            "shipment tracking", "return request", "refund processed", "invoice for order"
        ),
        SOCIAL to listOf(
            "friend request", "started following you", "invitation to connect",
            "mentioned you in", "commented on", "birthday"
        ),
        TECH to listOf(
            "pull request", "merge request", "pipeline failed", "build passed",
            "security alert", "access key", "verification code", "deployment"
        )
    )

    /**
     * Layer 2: Keyword Scoring
     * 
     * Sender Keywords: Lower weight (Bias). Identifies the *likely* category of the entity.
     * Context Keywords: Higher weight (Intent). Identifies what the email is *about*.
     */
    private val senderEntityKeywords = mapOf(
        FINANCE to listOf("bank", "pay", "wallet", "card", "finance", "invest", "mutual", "fund", "stock", "insurance", "loan", "tax", "gst", "hdfc", "sbi", "axis", "icici", "kotak", "pnb", "bob", "paytm", "phonepe", "razorpay", "gpay", "googlepay", "cred", "zerodha", "groww", "upstox", "indmoney", "dhan", "navi"),
        JOBS to listOf("linkedin", "naukri", "indeed", "glassdoor", "instahyre", "tophire", "wellfound", "angelist", "workday", "lever", "greenhouse", "foundit", "cutshort"),
        SHOPPING to listOf("amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa", "swiggy", "zomato", "blinkit", "zepto", "bigbasket", "jiomart", "tataneu", "snapdeal"),
        TRAVEL to listOf("uber", "ola", "rapido", "irctc", "makemytrip", "goibibo", "easemytrip", "indigo", "airindia", "vistara", "redbus", "ixigo", "booking", "agoda", "airbnb"),
        SOCIAL to listOf("facebook", "instagram", "twitter", "x", "pinterest", "snapchat", "reddit", "quora", "medium", "youtube", "twitch", "discord", "whatsapp", "telegram"),
        TECH to listOf("github", "gitlab", "bitbucket", "jira", "atlassian", "confluence", "trello", "slack", "notion", "figma", "canva", "google cloud", "aws", "azure", "firebase", "vercel", "netlify", "heroku"),
        ENTERTAINMENT to listOf("netflix", "prime video", "hotstar", "disney", "spotify", "apple music", "jiosaavn", "gaana", "bookmyshow", "insider"),
        EDUCATION to listOf("udemy", "coursera", "edx", "pluralsight", "codecademy", "udacity", "skillshare", "upgrad", "scaler", "simplilearn", "unacademy", "byjus")
    )

    private val contextKeywords = mapOf(
        FINANCE to listOf("invoice", "bill", "receipt", "premium", "ledger", "itr", "salary", "expense", "budget"),
        JOBS to listOf("career", "hiring", "resume", "cv", "opportunity", "vacancy", "opening", "job", "recruit", "interview", "offer"),
        SHOPPING to listOf("discount", "sale", "buy", "purchase", "cart", "shop", "store", "deal", "coupon", "cashback"),
        TRAVEL to listOf("ticket", "flight", "train", "bus", "cab", "ride", "driver", "vacation", "tour", "stay"),
        SOCIAL to listOf("post", "story", "status", "timeline", "feed", "message", "dm", "connection", "network"),
        TECH to listOf("code", "dev", "api", "sdk", "server", "database", "linux", "bug", "issue", "commit", "repo"),
        ENTERTAINMENT to listOf("movie", "series", "episode", "song", "album", "playlist", "concert", "event", "stream", "watch"),
        EDUCATION to listOf("course", "class", "lecture", "tutorial", "exam", "quiz", "assignment", "grade", "certificate", "learning")
    )

    // Weights configuration
    private val WEIGHT_STRONG_INTENT = 100 // Automatic win
    private val WEIGHT_SENDER_MATCH = 2    // Low weight for entity (reduces bias)
    private val WEIGHT_SUBJECT_MATCH = 5   // High weight for context
    private val WEIGHT_SNIPPET_MATCH = 3   // Medium weight for context

    fun classify(sender: String, subject: String, snippet: String): String {
        val cleanSender = sender.lowercase()
        val cleanSubject = subject.lowercase()
        val cleanSnippet = snippet.lowercase()
        val cleanContent = "$cleanSubject $cleanSnippet"

        // 1. Check Strong Intent (Overrides everything)
        for ((category, patterns) in strongIntentRules) {
            if (patterns.any { cleanContent.contains(it) }) {
                return category
            }
        }

        // 2. Scoring System
        val scores = mutableMapOf<String, Int>()
        
        // Initialize
        senderEntityKeywords.keys.forEach { scores[it] = 0 }

        // A. Score Context (Subject & Snippet) - Higher Priority
        for ((category, keywords) in contextKeywords) {
            for (keyword in keywords) {
                if (cleanSubject.contains(keyword)) {
                    scores[category] = (scores[category] ?: 0) + WEIGHT_SUBJECT_MATCH
                }
                if (cleanSnippet.contains(keyword)) {
                    scores[category] = (scores[category] ?: 0) + WEIGHT_SNIPPET_MATCH
                }
            }
        }

        // B. Score Sender - Lower Priority
        // We only add sender score if it matches, but since the weight is low (2),
        // it won't override a strong subject match (5).
        // Example: PayPal (Sender=2) + Job (Subject=5) -> Jobs Wins (5 > 2)
        for ((category, keywords) in senderEntityKeywords) {
            for (keyword in keywords) {
                if (cleanSender.contains(keyword)) {
                    scores[category] = (scores[category] ?: 0) + WEIGHT_SENDER_MATCH
                }
            }
        }

        // 3. Result Resolution
        val topMatches = scores.entries.sortedByDescending { it.value }.take(2)
        
        if (topMatches.isEmpty() || topMatches[0].value == 0) {
            // 4. Fallback Heuristics
            return if (cleanContent.contains("unsubscribe") || cleanContent.contains("newsletter")) {
                PROMOTIONS
            } else {
                OTHER
            }
        }

        val winner = topMatches[0]
        
        // Check for conflicts if scores are close (within 3 points)
        if (topMatches.size > 1) {
            val runnerUp = topMatches[1]
            if ((winner.value - runnerUp.value) <= 3) {
                return resolveConflict(winner.key, runnerUp.key, cleanContent)
            }
        }

        return winner.key
    }

    /**
     * Resolves ambiguity between two high-scoring categories using specific context rules.
     */
    private fun resolveConflict(cat1: String, cat2: String, content: String): String {
        val set = setOf(cat1, cat2)

        // 1. Finance vs Travel (e.g. Uber Receipt)
        if (set.contains(FINANCE) && set.contains(TRAVEL)) {
            // If it's explicitly a receipt/invoice, it's Finance. Otherwise, the service itself (Travel) wins.
            if (content.contains("receipt") || content.contains("invoice") || content.contains("bill")) {
                return FINANCE
            }
            return TRAVEL
        }

        // 2. Jobs vs Social (e.g. LinkedIn)
        if (set.contains(JOBS) && set.contains(SOCIAL)) {
            // "Job" intent is usually more specific and valuable than generic "Social" updates
            if (content.contains("hiring") || content.contains("apply") || content.contains("job")) {
                return JOBS
            }
            // Fallback to Social for things like "feed", "post", "birthday"
            return SOCIAL
        }

        // 3. Shopping vs Tech (e.g. Amazon/Google)
        if (set.contains(SHOPPING) && set.contains(TECH)) {
            if (content.contains("order") || content.contains("delivery") || content.contains("shipment")) {
                return SHOPPING
            }
            return TECH
        }

        // 4. Finance vs Shopping (e.g. Amazon Pay / Credit Card offers)
        if (set.contains(FINANCE) && set.contains(SHOPPING)) {
            if (content.contains("credit card") || content.contains("statement") || content.contains("balance")) {
                return FINANCE
            }
            return SHOPPING
        }

        // Default: Stick to the one with the slightly higher score (cat1 is the winner sorted by score)
        return cat1
    }
}
