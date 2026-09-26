package vn.ptit.one.auth.model;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import vn.ptit.one.auth.model.AccountRecord.Credential;
import vn.ptit.one.auth.model.AccountRecord.Source;

import static org.assertj.core.api.Assertions.assertThat;

class AccountRecordTest {

    private static AccountRecord student(String status, Credential credential) {
        return new AccountRecord("B26DCCN001", Role.SINH_VIEN, "HCM", "B26DCCN001", status, 1, credential);
    }

    private static Credential site(Role role, String campus, String entity) {
        return new Credential(Source.SITE, "{argon2id-v1}x", role, campus, entity, true);
    }

    @Test
    void activeConsistentAccountCanSignIn() {
        assertThat(student("HOAT_DONG", site(Role.SINH_VIEN, "HCM", "B26DCCN001")).canSignIn()).isTrue();
    }

    @Test
    void onlyActiveStatusCanSignIn() {
        for (String status : new String[] {"CHO_KICH_HOAT", "NGUNG", "DANG_CHUYEN"}) {
            assertThat(student(status, site(Role.SINH_VIEN, "HCM", "B26DCCN001")).canSignIn()).isFalse();
        }
    }

    @Test
    void mismatchBetweenDirectoryAndAccountIsRejectedNotEscalated() {
        assertThat(student("HOAT_DONG", site(Role.ADMIN_CO_SO, "HCM", "B26DCCN001")).canSignIn()).isFalse();
        assertThat(student("HOAT_DONG", site(Role.SINH_VIEN, "HN", "B26DCCN001")).canSignIn()).isFalse();
        assertThat(student("HOAT_DONG", site(Role.SINH_VIEN, "HCM", "B26DCCN999")).canSignIn()).isFalse();
        assertThat(student("HOAT_DONG", new Credential(Source.BOTH, null, null, null, null, false)).canSignIn()).isFalse();
        assertThat(student("HOAT_DONG", null).canSignIn()).isFalse();
    }

    @Test
    void masterMustComeFromMasterTableAndBeActive() {
        Credential master = new Credential(Source.MASTER, "{argon2id-v1}x", Role.ADMIN_MASTER, null, null, true);
        AccountRecord admin = new AccountRecord("admin.master", Role.ADMIN_MASTER, null, null, "HOAT_DONG", 1, master);
        assertThat(admin.canSignIn()).isTrue();

        Credential disabled = new Credential(Source.MASTER, "{argon2id-v1}x", Role.ADMIN_MASTER, null, null, false);
        assertThat(new AccountRecord("admin.master", Role.ADMIN_MASTER, null, null, "HOAT_DONG", 1, disabled)
                .canSignIn()).isFalse();
    }

    @Test
    void sessionSnapshotRejectsRevokedExpiredOrStaleVersion() {
        Instant now = Instant.parse("2026-09-26T00:00:00Z");
        SessionSnapshot live = new SessionSnapshot(UUID.randomUUID(), "B26DCCN001", now.plusSeconds(60), null,
                Role.SINH_VIEN, "HCM", "B26DCCN001", "HOAT_DONG", 2, 2);

        assertThat(live.accepts("B26DCCN001", 2, now)).isTrue();
        assertThat(live.accepts("B26DCCN001", 1, now)).as("version cũ sau logout-all").isFalse();
        assertThat(live.accepts("B26DCCN002", 2, now)).as("sid của người khác").isFalse();
        assertThat(live.accepts("B26DCCN001", 2, now.plusSeconds(60))).as("hết hạn").isFalse();

        SessionSnapshot revoked = new SessionSnapshot(live.sessionId(), "B26DCCN001", now.plusSeconds(60), now,
                Role.SINH_VIEN, "HCM", "B26DCCN001", "HOAT_DONG", 2, 2);
        assertThat(revoked.accepts("B26DCCN001", 2, now)).isFalse();

        SessionSnapshot locked = new SessionSnapshot(live.sessionId(), "B26DCCN001", now.plusSeconds(60), null,
                Role.SINH_VIEN, "HCM", "B26DCCN001", "NGUNG", 2, 2);
        assertThat(locked.accepts("B26DCCN001", 2, now)).isFalse();
    }

    @Test
    void refreshNeedsLiveSessionAndUnchangedVersion() {
        Instant now = Instant.parse("2026-09-26T00:00:00Z");
        UUID sid = UUID.randomUUID();
        assertThat(snapshot(sid, now.plusSeconds(60), null, "HOAT_DONG", 1, 1).canRefresh(now)).isTrue();
        assertThat(snapshot(sid, now.plusSeconds(60), null, "HOAT_DONG", 2, 1).canRefresh(now))
                .as("logout-all đã tăng phiên bản").isFalse();
        assertThat(snapshot(sid, now, null, "HOAT_DONG", 1, 1).canRefresh(now)).as("hết hạn tuyệt đối").isFalse();
        assertThat(snapshot(sid, now.plusSeconds(60), now, "HOAT_DONG", 1, 1).canRefresh(now)).isFalse();
        assertThat(snapshot(sid, now.plusSeconds(60), null, "NGUNG", 1, 1).canRefresh(now)).isFalse();
    }

    @Test
    void usedRefreshTokenIsReplayNotMerelyExpired() {
        Instant now = Instant.parse("2026-09-26T00:00:00Z");
        UUID id = UUID.randomUUID();
        RefreshTokenRecord fresh = new RefreshTokenRecord(id, id, now.plusSeconds(60), null, null);
        RefreshTokenRecord used = new RefreshTokenRecord(id, id, now.plusSeconds(60), now, null);

        assertThat(fresh.isUsable(now)).isTrue();
        assertThat(fresh.isReplay()).isFalse();
        assertThat(used.isUsable(now)).isFalse();
        assertThat(used.isReplay()).isTrue();
        assertThat(new RefreshTokenRecord(id, id, now, null, null).isUsable(now)).isFalse();
    }

    private static SessionSnapshot snapshot(UUID sid, Instant expiresAt, Instant revokedAt, String status,
            int accountVersion, int versionAtCreate) {
        return new SessionSnapshot(sid, "B26DCCN001", expiresAt, revokedAt, Role.SINH_VIEN, "HCM", "B26DCCN001",
                status, accountVersion, versionAtCreate);
    }
}
