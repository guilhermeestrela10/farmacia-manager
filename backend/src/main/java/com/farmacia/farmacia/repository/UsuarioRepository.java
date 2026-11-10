package com.farmacia.farmacia.repository;
import com.farmacia.farmacia.model.*;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
@Repository
public class UsuarioRepository extends MongoRepositoryBase<Usuario> {
    public UsuarioRepository(MongoTemplate mongo) { super(mongo, Usuario.class); }
    public Optional<Usuario> findByLogin(String login) { return Optional.ofNullable(mongo.findOne(by("login", login), type)); }
    public Optional<Usuario> buscarPorIdComTravamento(Long id) { return locked(by("id", id)); }
    public Optional<Usuario> buscarPorLoginComTravamento(String login) { return locked(by("login", login)); }
    public List<Usuario> buscarAdministradoresAtivosComTravamento() {
        adminGuard();
        return lockedAll(by("perfil", Perfil.ADMIN).addCriteria(Criteria.where("ativo").is(true)).with(Sort.by("id")));
    }
    public boolean existsByLogin(String login) { return mongo.exists(by("login", login), type); }
    public long countByPerfilAndAtivoTrue(Perfil perfil) { return count(by("perfil", perfil).addCriteria(Criteria.where("ativo").is(true))); }
    public List<Usuario> findAllByOrderByNome() { return findAll(Sort.by("nome")); }
}
