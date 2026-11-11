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
    public Page<ProductView> search(String group,String q,int page,boolean descending,boolean staff) {
        var comparator=Comparator.comparing(ProductView::price).thenComparing(ProductView::id);
        if(descending) comparator=comparator.reversed();
        String name = q.toLowerCase(Locale.ROOT).strip();
        var rows = repository.findForCatalog(name, staff).stream()
            .filter(p -> group.isBlank() || p.getCategory().equals(group))
            .map(ProductView::from).sorted(comparator).toList();
        var pageable=PageRequest.of(Math.max(0,page),12);
        int start=(int)Math.min(pageable.getOffset(),rows.size());
        return new PageImpl<>(rows.subList(start,Math.min(start+12,rows.size())),pageable,rows.size());
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
            repository.save(factory.create(f));
            return;
        }
        var p=get(id);
        if(p.getType()!=f.getType())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Không thể đổi loại sản phẩm.");
        p.update(f);
    }
    public void hide(Long id) {
        get(id).hide();
    }
}
