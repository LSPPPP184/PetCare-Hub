package com.petcare.service;
import com.petcare.domain.*;
import com.petcare.domain.factory.ProductFactory;
import com.petcare.dto.*;
import com.petcare.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional public class ProductService  {
    private final ProductRepository repository;
    private final ProductFactory factory;
    private final CommerceLocks locks;
    public Page<ProductView> search(String group,String q,int page,boolean descending,boolean staff) {
        var rows = repository.searchPage(q.toLowerCase(Locale.ROOT).strip(), group, staff, descending, PageRequest.of(page, 12));
        if (!rows.isEmpty()) repository.fetchPackageCoverage(rows.stream().map(Product::getId).toList());
        return rows.map(ProductView::summary);
    }

    public Product get(Long id) {
        return repository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy sản phẩm."));
    }
    public ProductView detail(Long id) {
        var p=get(id);
        if(!p.isActive())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy sản phẩm.");
        return ProductView.from(p);
    }
    public void save(Long id,ProductForm f) {
        if(id==null) {
            var p = factory.create(f);
            configureServices(p, f);
            repository.save(p);
            return;
        }
        get(id);
        var p=locks.lock(Product.class,id);
        if(!Objects.equals(f.getVersion(),p.getVersion()))throw new ProductVersionConflictException();
        if(p.getType()!=f.getType())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Không thể đổi loại sản phẩm.");
        p.updateCatalog(f);
        configureServices(p, f);
    }
    public ProductForm form(Long id) { return get(id).toForm(); }
    public List<AppointmentServiceView> bookableServices() {
        return repository.findAll().stream().filter(com.petcare.domain.appointment.AppointmentServices::supports)
            .map(com.petcare.domain.appointment.AppointmentServices::require).map(AppointmentServiceView::from).toList();
    }
    private void configureServices(Product product, ProductForm form) {
        var ids = form.getCoveredServiceIds() == null ? List.<Long>of() : form.getCoveredServiceIds();
        var selected = ids.stream().distinct().map(id -> {
            if (id == null || id <= 0) throw new com.petcare.domain.appointment.AppointmentException("Dịch vụ áp dụng không hợp lệ.");
            return repository.findById(id).orElseThrow(() ->
                new com.petcare.domain.appointment.AppointmentException("Dịch vụ áp dụng không tồn tại. Vui lòng chọn lại."));
        })
            .map(com.petcare.domain.appointment.AppointmentServices::require).toList();
        product.configureCoveredServices(selected);
    }
    public void hide(Long id) {
        get(id).hide();
    }
}
