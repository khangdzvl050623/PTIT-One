package vn.ptit.one.teacher.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

import vn.ptit.one.auth.service.AccountService;
import vn.ptit.one.shared.outbox.MasterOutboxHandler;
import vn.ptit.one.shared.outbox.OutboxStore.PendingEvent;
import vn.ptit.one.shared.routing.SiteContext;
import vn.ptit.one.teacher.model.TeacherCreated;

/** {@code GiangVienDuocTao}: cơ sở TRƯỚC, Master SAU — cùng lý do với sinh viên. */
@Component
@Profile("distributed")
public class TeacherCreatedHandler implements MasterOutboxHandler {

    private final TeacherProvisioningService provisioning;
    private final AccountService accounts;
    private final ObjectMapper json;

    public TeacherCreatedHandler(TeacherProvisioningService provisioning, AccountService accounts, ObjectMapper json) {
        this.provisioning = provisioning;
        this.accounts = accounts;
        this.json = json;
    }

    @Override
    public String loaiSuKien() {
        return TeacherCreated.LOAI;
    }

    @Override
    public void deliver(PendingEvent e) {
        TeacherCreated ev = json.readValue(e.noiDung(), TeacherCreated.class);
        SiteContext.runAt(ev.maCoSo(), () -> provisioning.materialize(ev));
        SiteContext.runAt(SiteContext.MASTER, () -> accounts.confirmProvisioned(ev.maGiangVien(), e.eventId()));
    }
}
