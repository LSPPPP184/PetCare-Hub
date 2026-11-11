package com.petcare.repository;
import com.petcare.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface ProductRepository extends JpaRepository<Product,Long> {
    @Query("select p from Product p where (:includeHidden = true or p.active = true) "
        + "and (:name = '' or locate(:name, lower(p.name)) > 0)")
    List<Product> findForCatalog(@Param("name") String name, @Param("includeHidden") boolean includeHidden);
}
