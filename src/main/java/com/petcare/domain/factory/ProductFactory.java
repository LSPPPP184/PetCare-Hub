package com.petcare.domain.factory;
import com.petcare.domain.*;
import com.petcare.dto.ProductForm;
import java.util.*;
import java.util.function.Function;
import org.springframework.stereotype.Component;
/** Factory tập trung khởi tạo từng lớp sản phẩm bằng bảng creator, tránh phân nhánh theo loại. */
@Component public class ProductFactory  {
    private final Map<ProductType,Function<ProductForm,Product>> creators=Map.of( ProductType.PET,f-> {
        var p=new Pet();
        p.update(f);
        return p;
    }
    , ProductType.ACCESSORY,f-> {
        var p=new Accessory();
        p.update(f);
        return p;
    }
    , ProductType.GROOMING,f-> {
        var p=new GroomingService();
        p.update(f);
        return p;
    }
    , ProductType.VET,f-> {
        var p=new VetService();
        p.update(f);
        return p;
    }
    , ProductType.MONTHLY,f-> {
        var p=new MonthlyCarePackage();
        p.update(f);
        return p;
    }
    );
    public Product create(ProductForm f) {
        return creators.get(Objects.requireNonNull(f.getType())).apply(f);
    }
}
