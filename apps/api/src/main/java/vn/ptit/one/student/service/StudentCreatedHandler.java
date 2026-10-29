package vn.ptit.one.student.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

import vn.ptit.one.auth.service.AccountService;
import vn.ptit.one.shared.outbox.MasterOutboxHandler;
import vn.ptit.one.shared.outbox.OutboxStore.PendingEvent;
import vn.ptit.one.shared.routing.SiteContext;
import vn.ptit.one.student.model.StudentCreated;

/**
 * {@code SinhVienDuocTao}: dựng hồ sơ ở cơ sở nhà TRƯỚC, mở danh bạ + đánh dấu
 * {@code SENT} tại Master SAU. Chết giữa hai bước thì lần sau làm lại — bước
 * một idempotent nên vô hại. Đảo thứ tự là mất sự kiện vĩnh viễn.
 */
@Component
@Profile("distributed")
public class StudentCreatedHandler implements MasterOutboxHandler {

    private final StudentProvisioningService provisioning;
    private final AccountService accounts;
    private final ObjectMapper json;

    public StudentCreatedHandler(StudentProvisioningService provisioning, AccountService accounts, ObjectMapper json) {
        this.provisioning = provisioning;
        this.accounts = accounts;
        this.json = json;
    }

    @Override
    public String loaiSuKien() {
        return StudentCreated.LOAI;
    }

    @Override
    public void deliver(PendingEvent e) {
        StudentCreated ev = json.readValue(e.noiDung(), StudentCreated.class);
        SiteContext.runAt(ev.maCoSoNha(), () -> provisioning.materialize(ev));
        SiteContext.runAt(SiteContext.MASTER, () -> accounts.confirmProvisioned(ev.maSinhVien(), e.eventId()));
    }
}
