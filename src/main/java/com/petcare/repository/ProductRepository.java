package com.petcare.repository;
import com.petcare.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface ProductRepository extends JpaRepository<Product,Long> {
    @Query("select p from Product p left join GroomingService g on g.id = p.id where (:staff = true or p.active = true) and (:name = '' or locate(:name, lower(p.name)) > 0) and (:group = '' or (:group = 'PET' and type(p) = Pet) or (:group = 'ACCESSORY' and type(p) = Accessory) or (:group = 'SERVICE' and (type(p) = GroomingService or type(p) = VetService or type(p) = MonthlyCarePackage))) order by case when :descending = false then p.basePrice + coalesce(g.petSizeSurcharge, 0) end asc, case when :descending = true then p.basePrice + coalesce(g.petSizeSurcharge, 0) end desc, case when :descending = false then p.id end asc, p.id desc")
    org.springframework.data.domain.Page<Product> searchPage(String name, String group, boolean staff, boolean descending, org.springframework.data.domain.Pageable pageable);
    @Query("select distinct p from MonthlyCarePackage p left join fetch p.coveredServices where p.id in :ids")
    List<com.petcare.domain.MonthlyCarePackage> fetchPackageCoverage(List<Long> ids);

    @Query("select p from Product p where (:includeHidden = true or p.active = true) "
        + "and (:name = '' or locate(:name, lower(p.name)) > 0)")
    List<Product> findForCatalog(@Param("name") String name, @Param("includeHidden") boolean includeHidden);
}
