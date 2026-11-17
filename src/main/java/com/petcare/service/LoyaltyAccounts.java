package com.petcare.service;

import com.petcare.domain.*;
import com.petcare.repository.LoyaltyAccountRepository;
import com.petcare.config.LoyaltyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

/** Khóa hàng User tồn tại cả trước khi mở thẻ, bảo vệ tạo thẻ, số dư và kiểm tra nguồn sự kiện. */
@Component @RequiredArgsConstructor
public class LoyaltyAccounts {
    private final CommerceLocks locks;
    private final LoyaltyAccountRepository accounts;
    private final LoyaltyProperties properties;

    /** Mở thẻ ở lần đầu cần dùng; thẻ đã tồn tại chỉ cần đọc, không lấy khóa ghi. */
    @Transactional(propagation = Propagation.MANDATORY)
    public LoyaltyAccount open(Long customerId) {
        return accounts.findByCustomerId(customerId).orElseGet(() -> locked(customerId));
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public LoyaltyAccount locked(Long customerId) {
        var customer = locks.lock(User.class, customerId);
        var account = accounts.findByCustomerId(customerId).orElse(null);
        var locked = account == null ? accounts.saveAndFlush(new LoyaltyAccount(customer)) : locks.lock(LoyaltyAccount.class, account.getId());
        locked.synchronizeTier(properties);
        return locked;
    }
}
