package com.tim14.slagalica.service;

import android.graphics.Color;

import com.tim14.slagalica.R;
import com.tim14.slagalica.LeagueUtils;
import com.tim14.slagalica.model.User;
import java.util.Locale;

public class ProfileService {

    public static final class ProfileUiData {
        private final String username;
        private final String email;
        private final String region;
        private final String tokens;
        private final String stars;
        private final String avatarFrame;
        private final int avatarFrameColor;
        private final int avatarResourceId;
        private final int leagueNameResId;
        private final String leagueIcon;
        private final String qrPayload;
        private final String qrLabel;

        public ProfileUiData(
                String username,
                String email,
                String region,
                String tokens,
                String stars,
                String avatarFrame,
                int avatarFrameColor,
                int avatarResourceId,
                int leagueNameResId,
                String leagueIcon,
                String qrPayload,
                String qrLabel
        ) {
            this.username = username;
            this.email = email;
            this.region = region;
            this.tokens = tokens;
            this.stars = stars;
            this.avatarFrame = avatarFrame;
            this.avatarFrameColor = avatarFrameColor;
            this.avatarResourceId = avatarResourceId;
            this.leagueNameResId = leagueNameResId;
            this.leagueIcon = leagueIcon;
            this.qrPayload = qrPayload;
            this.qrLabel = qrLabel;
        }

        public String getUsername() {
            return username;
        }

        public String getEmail() {
            return email;
        }

        public String getRegion() {
            return region;
        }

        public String getTokens() {
            return tokens;
        }

        public String getStars() {
            return stars;
        }

        public String getAvatarFrame() {
            return avatarFrame;
        }

        public int getAvatarFrameColor() {
            return avatarFrameColor;
        }

        public int getAvatarResourceId() {
            return avatarResourceId;
        }

        public int getLeagueNameResId() {
            return leagueNameResId;
        }

        public String getLeagueIcon() {
            return leagueIcon;
        }

        public String getQrPayload() {
            return qrPayload;
        }

        public String getQrLabel() {
            return qrLabel;
        }
    }

    public ProfileUiData prepareProfile(User user) {
        if (user == null) {
            user = new User();
        }

        String username = valueOrFallback(user.username, "Test User");
        String email = valueOrFallback(user.email, "test@example.com");
        String region = valueOrFallback(user.region, "Serbia");
        String avatarFrame = sanitizeAvatarFrame(user.avatarFrame);
        LeagueUi leagueUi = getLeagueUi(user.league);
        String qrPayload = createQrPayload(user);

        return new ProfileUiData(
                username,
                email,
                region,
                String.valueOf(user.tokens),
                String.valueOf(user.stars),
                avatarFrame,
                getAvatarFrameColor(avatarFrame),
                getAvatarResource(valueOrFallback(user.avatar, "avatar_1")),
                leagueUi.nameResId,
                leagueUi.icon,
                qrPayload,
                username
        );
    }

    public int getAvatarResource(String avatar) {
        switch (valueOrFallback(avatar, "avatar_1")) {
            case "avatar_2":
                return R.drawable.avatar_2;
            case "avatar_3":
                return R.drawable.avatar_3;
            case "avatar_4":
                return R.drawable.avatar_4;
            case "avatar_5":
                return R.drawable.avatar_5;
            case "avatar_6":
                return R.drawable.avatar_6;
            case "avatar_1":
            default:
                return R.drawable.avatar_1;
        }
    }

    public String sanitizeAvatarFrame(String avatarFrame) {
        String normalized = valueOrFallback(avatarFrame, "None");

        if (normalized.equalsIgnoreCase("Bronze")
                || normalized.equalsIgnoreCase("Silver")
                || normalized.equalsIgnoreCase("Gold")) {
            return normalized.substring(0, 1).toUpperCase(Locale.US)
                    + normalized.substring(1).toLowerCase(Locale.US);
        }

        return "None";
    }

    public int getAvatarFrameColor(String avatarFrame) {
        switch (sanitizeAvatarFrame(avatarFrame).toLowerCase(Locale.US)) {
            case "bronze":
                return Color.rgb(176, 105, 45);
            case "silver":
                return Color.rgb(192, 192, 192);
            case "gold":
                return Color.rgb(255, 196, 32);
            case "none":
            default:
                return Color.TRANSPARENT;
        }
    }

    public String createQrPayload(User user) {
        if (user == null) {
            return "unknown_user";
        }

        return valueOrFallback(
                user.qrCode,
                valueOrFallback(user.id, "unknown_user")
        );
    }

    private LeagueUi getLeagueUi(int league) {
        switch (league) {
            case 1:
                return new LeagueUi(R.string.bronze_league, LeagueUtils.getLeagueIcon(league));
            case 2:
                return new LeagueUi(R.string.silver_league, LeagueUtils.getLeagueIcon(league));
            case 3:
                return new LeagueUi(R.string.gold_league, LeagueUtils.getLeagueIcon(league));
            case 4:
                return new LeagueUi(R.string.diamond_league, LeagueUtils.getLeagueIcon(league));
            case 5:
                return new LeagueUi(R.string.master_league, LeagueUtils.getLeagueIcon(league));
            case 0:
            default:
                return new LeagueUi(R.string.no_league, LeagueUtils.getLeagueIcon(league));
        }
    }

    private String valueOrFallback(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value;
    }

    private static final class LeagueUi {
        private final int nameResId;
        private final String icon;

        private LeagueUi(int nameResId, String icon) {
            this.nameResId = nameResId;
            this.icon = icon;
        }
    }
}
