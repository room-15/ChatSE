package com.tristanwiley.chatse.database

import io.realm.Realm
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

/**
 * Created by jaroslawmichalik on 11.10.2017
 */
class UserRepository : Repository<String, User> {

    val myId : String = "1"

    val toPojoMapper: Mapper<RealmUser, User> = RealmUserMapper()
    val toRealmMapper: Mapper<User, RealmUser> = UserRealmMapper()

    override fun findAll(): List<User> = Realm.getDefaultInstance()
            .use { return it.where(RealmUser::class.java).findAll().map(toPojoMapper::map) }

    override fun findOne(id: String): User? = Realm.getDefaultInstance()
            .use {
                return it.where(RealmUser::class.java)
                        .equalTo("me", "1")
                        .findFirst()?.let(toPojoMapper::map)
            }

    override fun findAllBy(spec: Specification) = listOf<User>()

    override fun insert(t: User) = toRealmMapper.map(t).let { t1 ->
        Realm.getDefaultInstance().use {
            it.executeTransaction { realm -> realm.insertOrUpdate(t1) }
        }
    }

    override fun delete(id: String) = Realm.getDefaultInstance().use {
        it.executeTransaction { realm -> realm.where(RealmUser::class.java).equalTo("id", id).findFirst().deleteFromRealm() }
    }

    override fun deleteAll() = Realm.getDefaultInstance().use {
        it.executeTransaction { realm -> realm.delete(RealmUser::class.java) }
    }

}

class RealmUserMapper : Mapper<RealmUser, User> {
    override fun map(f: RealmUser): User = User(f.soid, f.seid, f.name, f.email)

}

class UserRealmMapper : Mapper<User, RealmUser> {
    override fun map(f: User): RealmUser = RealmUser(f.soid, f.seID, f.name, f.email)
}


data class User(var soid: Int = -1, var seID: Int = -1, var name: String = "", var email: String = "")

@RealmClass
open class RealmUser(
        var soid: Int = -1,
        var seid: Int = -1,
        var name: String = "",
        var email: String = "") : RealmObject(){

    @PrimaryKey
    var me: String = "1" //Realm 'singleton'
}