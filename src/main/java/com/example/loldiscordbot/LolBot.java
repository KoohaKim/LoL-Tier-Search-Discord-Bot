package com.example.loldiscordbot;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LolBot extends ListenerAdapter {
    private static final String RIOT_API_KEY = System.getenv("RIOT_API_KEY");
    private static final String RIOT_API_BASE_URL = "https://kr.api.riotgames.com/";

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        String content = event.getMessage().getContentRaw();
        Pattern commandPattern = Pattern.compile("!티어캣\\s*(.*)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = commandPattern.matcher(content);

        if (matcher.matches()) {
            // matcher.group(1)을 통해 정규 표현식의 첫 번째 그룹을 가져오기.
            String trimmedContent = matcher.group(1).replaceAll("\\s", "%20");

            // 만약 #이 있다면
            if (trimmedContent.contains("#")) {
                // "#"을 기준으로 문자열을 두 부분으로 나누기
                String[] parts = trimmedContent.split("#", 2);

                String gameName = parts[0].trim();
                String tagLine = parts[1].trim();

                event.getChannel().sendMessage(getSummonerData(gameName, tagLine)).queue();
            } else {
                // "#"이 없다면 기본적으로 tagLine을 "KR1"로 설정
                String gameName = trimmedContent;
                String tagLine = "KR1";

                event.getChannel().sendMessage(getSummonerData(gameName, tagLine)).queue();
            }
        }
    }

    // puuid 가져오기
    private String getSummonerData(String gameName, String tagLine) {
        try {
            String summonerApiUrl = GetPuuIdApiUrl(gameName, tagLine);
            String responseBody = fetchApiData(summonerApiUrl);

            if (responseBody != null) {
                JSONObject summonerJson = new JSONObject(responseBody);
                String puuId = summonerJson.getString("puuid");

                // puuid를 사용하여 소환사 정보 가져오기
                String summonerInfo = getSummonerInfo(puuId);
                return summonerInfo;
            } else {
                return "해당 소환사는 존재하지 않습니다. 닉네임 혹은 태그를 확인해주세요!";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "소환사 정보를 가져오는 도중 오류가 발생했습니다.";
        }
    }

    // 소환사 정보 가져오기
    private String getSummonerInfo(String puuId) {
        try {
            String summonerApiUrl = SummonerDataApiUrl(puuId);
            String responseBody = fetchApiData(summonerApiUrl);

            if (responseBody != null) {
                JSONObject summonerJson = new JSONObject(responseBody);
                String summonerId = summonerJson.getString("id");
                int summonerLevel = summonerJson.getInt("summonerLevel");
                String summonerName = summonerJson.getString("name");

                String rankedData = getRankedData(summonerId);

                return "***소환사명***  :  " + summonerName +
                        "\n***소환사 레벨***  :  " + summonerLevel +
                        "\n***랭크 전적***  \n" + rankedData;
            } else {
                return "소환사 정보를 가져오는 도중 오류가 발생했습니다.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "소환사 정보를 가져오는 도중 오류가 발생했습니다.";
        }
    }

    // 랭크 데이터 가져오기
    private String getRankedData(String summonerId) {
        try {
            String rankedApiUrl = RankedApiUrl(summonerId);
            String responseBody = fetchApiData(rankedApiUrl);

            if (responseBody != null) {
                JSONArray rankedArray = new JSONArray(responseBody);
                StringBuilder result = new StringBuilder();

                for (int i = 0; i < rankedArray.length(); i++) {
                    JSONObject entry = rankedArray.getJSONObject(i);

                    if (entry.has("tier")) {
                        String queueType = entry.getString("queueType");
                        String tier = entry.getString("tier");
                        String rank = entry.getString("rank");
                        int lp = entry.getInt("leaguePoints");

                        // 레이블 변경
                        String modifiedQueueType = getModifiedQueueType(queueType);

                        result.append(modifiedQueueType).append(" ").append(tier).append(" ").append(rank).append(" ").append(lp).append(" LP\n");
                    }
                }

                return result.toString();
            } else {
                return "랭크 데이터를 가져오는 도중 오류가 발생했습니다.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "랭크 데이터를 가져오는 도중 오류가 발생했습니다.";
        }
    }

    private String getModifiedQueueType(String originalQueueType) {
        // 레이블 변경 메소드
        switch (originalQueueType) {
            case "RANKED_SOLO_5x5":
                return "솔로랭크:";
            case "RANKED_FLEX_SR":
                return "자유랭크:";
            default:
                return originalQueueType;
        }
    }

    // API URL 구성 메서드
    private String GetPuuIdApiUrl(String summonerName, String tagLine) {
        return "https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/" + summonerName + "/" + tagLine + "?api_key=" + RIOT_API_KEY;
    }

    private String SummonerDataApiUrl(String summonerPuuId) {
        return RIOT_API_BASE_URL + "lol/summoner/v4/summoners/by-puuid/" + summonerPuuId + "?api_key=" + RIOT_API_KEY;
    }

    private String RankedApiUrl(String summonerId) {
        return RIOT_API_BASE_URL + "lol/league/v4/entries/by-summoner/" + summonerId + "?api_key=" + RIOT_API_KEY;
    }

    // API 요청 메서드
    private String fetchApiData(String urlString) {
        try (InputStream response = new URL(urlString).openStream(); Scanner scanner = new Scanner(response)) {
            return scanner.useDelimiter("\\A").next();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}