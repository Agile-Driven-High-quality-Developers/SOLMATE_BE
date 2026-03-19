package org.solmate.external.ls.dto.websocket;

public record LsWsRequest(Header header, Body body) {

    public record Header(String token, String tr_type) {}

    public record Body(String tr_cd, String tr_key) {}

    public static LsWsRequest subscribe(String token, String stockCode) {
        return new LsWsRequest(
                new Header(token, "3"),
                new Body("S3_", stockCode)
        );
    }

    public static LsWsRequest unsubscribe(String token, String stockCode) {
        return new LsWsRequest(
                new Header(token, "4"),
                new Body("S3_", stockCode)
        );
    }
}
