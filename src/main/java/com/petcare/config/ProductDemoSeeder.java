package com.petcare.config;
import com.petcare.domain.*;
import com.petcare.dto.ProductForm;
import com.petcare.domain.factory.ProductFactory;
import com.petcare.repository.ProductRepository;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;
@Component @Profile("demo") @RequiredArgsConstructor public class ProductDemoSeeder  {
    private final ProductRepository repository;
    private final ProductFactory factory;
    @Transactional public void run(String... args) {
        if(repository.count()!=0)return;
        add(ProductType.PET,6,"Thú cưng");
        add(ProductType.ACCESSORY,8,"Phụ kiện");
        add(ProductType.GROOMING,2,"Tắm tỉa");
        add(ProductType.VET,2,"Khám thú y");
        add(ProductType.MONTHLY,1,"Gói tháng");
    }
    private void add(ProductType type,int count,String label) {
        for(int i=1;
        i<=count;
        i++) {
            var f=new ProductForm();
            f.setType(type);
            f.setName(label+" "+i);
            f.setDescription("Sản phẩm minh họa PetCare Hub");
            f.setBasePrice(new BigDecimal("100000").multiply(BigDecimal.valueOf(i)));
            f.setDoctorName("Bác sĩ Nguyễn An");
            f.setDurationMinutes(30);
            f.setDurationMonths(1);
            f.setIncludedUses(4);
            f.setIncludedServices("Tắm và chăm sóc");
            f.setStockQuantity(10);
            var product = factory.create(f);
            product.assignSeedKey("DEMO:PRODUCT:" + type.name() + ":" + i);
            repository.save(product);
        }
    }
}
