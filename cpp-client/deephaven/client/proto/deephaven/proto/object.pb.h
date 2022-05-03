// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: deephaven/proto/object.proto

#ifndef GOOGLE_PROTOBUF_INCLUDED_deephaven_2fproto_2fobject_2eproto
#define GOOGLE_PROTOBUF_INCLUDED_deephaven_2fproto_2fobject_2eproto

#include <limits>
#include <string>

#include <google/protobuf/port_def.inc>
#if PROTOBUF_VERSION < 3018000
#error This file was generated by a newer version of protoc which is
#error incompatible with your Protocol Buffer headers. Please update
#error your headers.
#endif
#if 3018000 < PROTOBUF_MIN_PROTOC_VERSION
#error This file was generated by an older version of protoc which is
#error incompatible with your Protocol Buffer headers. Please
#error regenerate this file with a newer version of protoc.
#endif

#include <google/protobuf/port_undef.inc>
#include <google/protobuf/io/coded_stream.h>
#include <google/protobuf/arena.h>
#include <google/protobuf/arenastring.h>
#include <google/protobuf/generated_message_table_driven.h>
#include <google/protobuf/generated_message_util.h>
#include <google/protobuf/metadata_lite.h>
#include <google/protobuf/generated_message_reflection.h>
#include <google/protobuf/message.h>
#include <google/protobuf/repeated_field.h>  // IWYU pragma: export
#include <google/protobuf/extension_set.h>  // IWYU pragma: export
#include <google/protobuf/unknown_field_set.h>
#include "deephaven/proto/ticket.pb.h"
// @@protoc_insertion_point(includes)
#include <google/protobuf/port_def.inc>
#define PROTOBUF_INTERNAL_EXPORT_deephaven_2fproto_2fobject_2eproto
PROTOBUF_NAMESPACE_OPEN
namespace internal {
class AnyMetadata;
}  // namespace internal
PROTOBUF_NAMESPACE_CLOSE

// Internal implementation detail -- do not use these members.
struct TableStruct_deephaven_2fproto_2fobject_2eproto {
  static const ::PROTOBUF_NAMESPACE_ID::internal::ParseTableField entries[]
    PROTOBUF_SECTION_VARIABLE(protodesc_cold);
  static const ::PROTOBUF_NAMESPACE_ID::internal::AuxiliaryParseTableField aux[]
    PROTOBUF_SECTION_VARIABLE(protodesc_cold);
  static const ::PROTOBUF_NAMESPACE_ID::internal::ParseTable schema[2]
    PROTOBUF_SECTION_VARIABLE(protodesc_cold);
  static const ::PROTOBUF_NAMESPACE_ID::internal::FieldMetadata field_metadata[];
  static const ::PROTOBUF_NAMESPACE_ID::internal::SerializationTable serialization_table[];
  static const ::PROTOBUF_NAMESPACE_ID::uint32 offsets[];
};
extern const ::PROTOBUF_NAMESPACE_ID::internal::DescriptorTable descriptor_table_deephaven_2fproto_2fobject_2eproto;
namespace io {
namespace deephaven {
namespace proto {
namespace backplane {
namespace grpc {
class FetchObjectRequest;
struct FetchObjectRequestDefaultTypeInternal;
extern FetchObjectRequestDefaultTypeInternal _FetchObjectRequest_default_instance_;
class FetchObjectResponse;
struct FetchObjectResponseDefaultTypeInternal;
extern FetchObjectResponseDefaultTypeInternal _FetchObjectResponse_default_instance_;
}  // namespace grpc
}  // namespace backplane
}  // namespace proto
}  // namespace deephaven
}  // namespace io
PROTOBUF_NAMESPACE_OPEN
template<> ::io::deephaven::proto::backplane::grpc::FetchObjectRequest* Arena::CreateMaybeMessage<::io::deephaven::proto::backplane::grpc::FetchObjectRequest>(Arena*);
template<> ::io::deephaven::proto::backplane::grpc::FetchObjectResponse* Arena::CreateMaybeMessage<::io::deephaven::proto::backplane::grpc::FetchObjectResponse>(Arena*);
PROTOBUF_NAMESPACE_CLOSE
namespace io {
namespace deephaven {
namespace proto {
namespace backplane {
namespace grpc {

// ===================================================================

class FetchObjectRequest final :
    public ::PROTOBUF_NAMESPACE_ID::Message /* @@protoc_insertion_point(class_definition:io.deephaven.proto.backplane.grpc.FetchObjectRequest) */ {
 public:
  inline FetchObjectRequest() : FetchObjectRequest(nullptr) {}
  ~FetchObjectRequest() override;
  explicit constexpr FetchObjectRequest(::PROTOBUF_NAMESPACE_ID::internal::ConstantInitialized);

  FetchObjectRequest(const FetchObjectRequest& from);
  FetchObjectRequest(FetchObjectRequest&& from) noexcept
    : FetchObjectRequest() {
    *this = ::std::move(from);
  }

  inline FetchObjectRequest& operator=(const FetchObjectRequest& from) {
    CopyFrom(from);
    return *this;
  }
  inline FetchObjectRequest& operator=(FetchObjectRequest&& from) noexcept {
    if (this == &from) return *this;
    if (GetOwningArena() == from.GetOwningArena()
  #ifdef PROTOBUF_FORCE_COPY_IN_MOVE
        && GetOwningArena() != nullptr
  #endif  // !PROTOBUF_FORCE_COPY_IN_MOVE
    ) {
      InternalSwap(&from);
    } else {
      CopyFrom(from);
    }
    return *this;
  }

  static const ::PROTOBUF_NAMESPACE_ID::Descriptor* descriptor() {
    return GetDescriptor();
  }
  static const ::PROTOBUF_NAMESPACE_ID::Descriptor* GetDescriptor() {
    return default_instance().GetMetadata().descriptor;
  }
  static const ::PROTOBUF_NAMESPACE_ID::Reflection* GetReflection() {
    return default_instance().GetMetadata().reflection;
  }
  static const FetchObjectRequest& default_instance() {
    return *internal_default_instance();
  }
  static inline const FetchObjectRequest* internal_default_instance() {
    return reinterpret_cast<const FetchObjectRequest*>(
               &_FetchObjectRequest_default_instance_);
  }
  static constexpr int kIndexInFileMessages =
    0;

  friend void swap(FetchObjectRequest& a, FetchObjectRequest& b) {
    a.Swap(&b);
  }
  inline void Swap(FetchObjectRequest* other) {
    if (other == this) return;
    if (GetOwningArena() == other->GetOwningArena()) {
      InternalSwap(other);
    } else {
      ::PROTOBUF_NAMESPACE_ID::internal::GenericSwap(this, other);
    }
  }
  void UnsafeArenaSwap(FetchObjectRequest* other) {
    if (other == this) return;
    GOOGLE_DCHECK(GetOwningArena() == other->GetOwningArena());
    InternalSwap(other);
  }

  // implements Message ----------------------------------------------

  inline FetchObjectRequest* New() const final {
    return new FetchObjectRequest();
  }

  FetchObjectRequest* New(::PROTOBUF_NAMESPACE_ID::Arena* arena) const final {
    return CreateMaybeMessage<FetchObjectRequest>(arena);
  }
  using ::PROTOBUF_NAMESPACE_ID::Message::CopyFrom;
  void CopyFrom(const FetchObjectRequest& from);
  using ::PROTOBUF_NAMESPACE_ID::Message::MergeFrom;
  void MergeFrom(const FetchObjectRequest& from);
  private:
  static void MergeImpl(::PROTOBUF_NAMESPACE_ID::Message* to, const ::PROTOBUF_NAMESPACE_ID::Message& from);
  public:
  PROTOBUF_ATTRIBUTE_REINITIALIZES void Clear() final;
  bool IsInitialized() const final;

  size_t ByteSizeLong() const final;
  const char* _InternalParse(const char* ptr, ::PROTOBUF_NAMESPACE_ID::internal::ParseContext* ctx) final;
  ::PROTOBUF_NAMESPACE_ID::uint8* _InternalSerialize(
      ::PROTOBUF_NAMESPACE_ID::uint8* target, ::PROTOBUF_NAMESPACE_ID::io::EpsCopyOutputStream* stream) const final;
  int GetCachedSize() const final { return _cached_size_.Get(); }

  private:
  void SharedCtor();
  void SharedDtor();
  void SetCachedSize(int size) const final;
  void InternalSwap(FetchObjectRequest* other);
  friend class ::PROTOBUF_NAMESPACE_ID::internal::AnyMetadata;
  static ::PROTOBUF_NAMESPACE_ID::StringPiece FullMessageName() {
    return "io.deephaven.proto.backplane.grpc.FetchObjectRequest";
  }
  protected:
  explicit FetchObjectRequest(::PROTOBUF_NAMESPACE_ID::Arena* arena,
                       bool is_message_owned = false);
  private:
  static void ArenaDtor(void* object);
  inline void RegisterArenaDtor(::PROTOBUF_NAMESPACE_ID::Arena* arena);
  public:

  static const ClassData _class_data_;
  const ::PROTOBUF_NAMESPACE_ID::Message::ClassData*GetClassData() const final;

  ::PROTOBUF_NAMESPACE_ID::Metadata GetMetadata() const final;

  // nested types ----------------------------------------------------

  // accessors -------------------------------------------------------

  enum : int {
    kSourceIdFieldNumber = 1,
  };
  // .io.deephaven.proto.backplane.grpc.TypedTicket source_id = 1;
  bool has_source_id() const;
  private:
  bool _internal_has_source_id() const;
  public:
  void clear_source_id();
  const ::io::deephaven::proto::backplane::grpc::TypedTicket& source_id() const;
  PROTOBUF_MUST_USE_RESULT ::io::deephaven::proto::backplane::grpc::TypedTicket* release_source_id();
  ::io::deephaven::proto::backplane::grpc::TypedTicket* mutable_source_id();
  void set_allocated_source_id(::io::deephaven::proto::backplane::grpc::TypedTicket* source_id);
  private:
  const ::io::deephaven::proto::backplane::grpc::TypedTicket& _internal_source_id() const;
  ::io::deephaven::proto::backplane::grpc::TypedTicket* _internal_mutable_source_id();
  public:
  void unsafe_arena_set_allocated_source_id(
      ::io::deephaven::proto::backplane::grpc::TypedTicket* source_id);
  ::io::deephaven::proto::backplane::grpc::TypedTicket* unsafe_arena_release_source_id();

  // @@protoc_insertion_point(class_scope:io.deephaven.proto.backplane.grpc.FetchObjectRequest)
 private:
  class _Internal;

  template <typename T> friend class ::PROTOBUF_NAMESPACE_ID::Arena::InternalHelper;
  typedef void InternalArenaConstructable_;
  typedef void DestructorSkippable_;
  ::io::deephaven::proto::backplane::grpc::TypedTicket* source_id_;
  mutable ::PROTOBUF_NAMESPACE_ID::internal::CachedSize _cached_size_;
  friend struct ::TableStruct_deephaven_2fproto_2fobject_2eproto;
};
// -------------------------------------------------------------------

class FetchObjectResponse final :
    public ::PROTOBUF_NAMESPACE_ID::Message /* @@protoc_insertion_point(class_definition:io.deephaven.proto.backplane.grpc.FetchObjectResponse) */ {
 public:
  inline FetchObjectResponse() : FetchObjectResponse(nullptr) {}
  ~FetchObjectResponse() override;
  explicit constexpr FetchObjectResponse(::PROTOBUF_NAMESPACE_ID::internal::ConstantInitialized);

  FetchObjectResponse(const FetchObjectResponse& from);
  FetchObjectResponse(FetchObjectResponse&& from) noexcept
    : FetchObjectResponse() {
    *this = ::std::move(from);
  }

  inline FetchObjectResponse& operator=(const FetchObjectResponse& from) {
    CopyFrom(from);
    return *this;
  }
  inline FetchObjectResponse& operator=(FetchObjectResponse&& from) noexcept {
    if (this == &from) return *this;
    if (GetOwningArena() == from.GetOwningArena()
  #ifdef PROTOBUF_FORCE_COPY_IN_MOVE
        && GetOwningArena() != nullptr
  #endif  // !PROTOBUF_FORCE_COPY_IN_MOVE
    ) {
      InternalSwap(&from);
    } else {
      CopyFrom(from);
    }
    return *this;
  }

  static const ::PROTOBUF_NAMESPACE_ID::Descriptor* descriptor() {
    return GetDescriptor();
  }
  static const ::PROTOBUF_NAMESPACE_ID::Descriptor* GetDescriptor() {
    return default_instance().GetMetadata().descriptor;
  }
  static const ::PROTOBUF_NAMESPACE_ID::Reflection* GetReflection() {
    return default_instance().GetMetadata().reflection;
  }
  static const FetchObjectResponse& default_instance() {
    return *internal_default_instance();
  }
  static inline const FetchObjectResponse* internal_default_instance() {
    return reinterpret_cast<const FetchObjectResponse*>(
               &_FetchObjectResponse_default_instance_);
  }
  static constexpr int kIndexInFileMessages =
    1;

  friend void swap(FetchObjectResponse& a, FetchObjectResponse& b) {
    a.Swap(&b);
  }
  inline void Swap(FetchObjectResponse* other) {
    if (other == this) return;
    if (GetOwningArena() == other->GetOwningArena()) {
      InternalSwap(other);
    } else {
      ::PROTOBUF_NAMESPACE_ID::internal::GenericSwap(this, other);
    }
  }
  void UnsafeArenaSwap(FetchObjectResponse* other) {
    if (other == this) return;
    GOOGLE_DCHECK(GetOwningArena() == other->GetOwningArena());
    InternalSwap(other);
  }

  // implements Message ----------------------------------------------

  inline FetchObjectResponse* New() const final {
    return new FetchObjectResponse();
  }

  FetchObjectResponse* New(::PROTOBUF_NAMESPACE_ID::Arena* arena) const final {
    return CreateMaybeMessage<FetchObjectResponse>(arena);
  }
  using ::PROTOBUF_NAMESPACE_ID::Message::CopyFrom;
  void CopyFrom(const FetchObjectResponse& from);
  using ::PROTOBUF_NAMESPACE_ID::Message::MergeFrom;
  void MergeFrom(const FetchObjectResponse& from);
  private:
  static void MergeImpl(::PROTOBUF_NAMESPACE_ID::Message* to, const ::PROTOBUF_NAMESPACE_ID::Message& from);
  public:
  PROTOBUF_ATTRIBUTE_REINITIALIZES void Clear() final;
  bool IsInitialized() const final;

  size_t ByteSizeLong() const final;
  const char* _InternalParse(const char* ptr, ::PROTOBUF_NAMESPACE_ID::internal::ParseContext* ctx) final;
  ::PROTOBUF_NAMESPACE_ID::uint8* _InternalSerialize(
      ::PROTOBUF_NAMESPACE_ID::uint8* target, ::PROTOBUF_NAMESPACE_ID::io::EpsCopyOutputStream* stream) const final;
  int GetCachedSize() const final { return _cached_size_.Get(); }

  private:
  void SharedCtor();
  void SharedDtor();
  void SetCachedSize(int size) const final;
  void InternalSwap(FetchObjectResponse* other);
  friend class ::PROTOBUF_NAMESPACE_ID::internal::AnyMetadata;
  static ::PROTOBUF_NAMESPACE_ID::StringPiece FullMessageName() {
    return "io.deephaven.proto.backplane.grpc.FetchObjectResponse";
  }
  protected:
  explicit FetchObjectResponse(::PROTOBUF_NAMESPACE_ID::Arena* arena,
                       bool is_message_owned = false);
  private:
  static void ArenaDtor(void* object);
  inline void RegisterArenaDtor(::PROTOBUF_NAMESPACE_ID::Arena* arena);
  public:

  static const ClassData _class_data_;
  const ::PROTOBUF_NAMESPACE_ID::Message::ClassData*GetClassData() const final;

  ::PROTOBUF_NAMESPACE_ID::Metadata GetMetadata() const final;

  // nested types ----------------------------------------------------

  // accessors -------------------------------------------------------

  enum : int {
    kTypedExportIdFieldNumber = 3,
    kTypeFieldNumber = 1,
    kDataFieldNumber = 2,
  };
  // repeated .io.deephaven.proto.backplane.grpc.TypedTicket typed_export_id = 3;
  int typed_export_id_size() const;
  private:
  int _internal_typed_export_id_size() const;
  public:
  void clear_typed_export_id();
  ::io::deephaven::proto::backplane::grpc::TypedTicket* mutable_typed_export_id(int index);
  ::PROTOBUF_NAMESPACE_ID::RepeatedPtrField< ::io::deephaven::proto::backplane::grpc::TypedTicket >*
      mutable_typed_export_id();
  private:
  const ::io::deephaven::proto::backplane::grpc::TypedTicket& _internal_typed_export_id(int index) const;
  ::io::deephaven::proto::backplane::grpc::TypedTicket* _internal_add_typed_export_id();
  public:
  const ::io::deephaven::proto::backplane::grpc::TypedTicket& typed_export_id(int index) const;
  ::io::deephaven::proto::backplane::grpc::TypedTicket* add_typed_export_id();
  const ::PROTOBUF_NAMESPACE_ID::RepeatedPtrField< ::io::deephaven::proto::backplane::grpc::TypedTicket >&
      typed_export_id() const;

  // string type = 1;
  void clear_type();
  const std::string& type() const;
  template <typename ArgT0 = const std::string&, typename... ArgT>
  void set_type(ArgT0&& arg0, ArgT... args);
  std::string* mutable_type();
  PROTOBUF_MUST_USE_RESULT std::string* release_type();
  void set_allocated_type(std::string* type);
  private:
  const std::string& _internal_type() const;
  inline PROTOBUF_ALWAYS_INLINE void _internal_set_type(const std::string& value);
  std::string* _internal_mutable_type();
  public:

  // bytes data = 2;
  void clear_data();
  const std::string& data() const;
  template <typename ArgT0 = const std::string&, typename... ArgT>
  void set_data(ArgT0&& arg0, ArgT... args);
  std::string* mutable_data();
  PROTOBUF_MUST_USE_RESULT std::string* release_data();
  void set_allocated_data(std::string* data);
  private:
  const std::string& _internal_data() const;
  inline PROTOBUF_ALWAYS_INLINE void _internal_set_data(const std::string& value);
  std::string* _internal_mutable_data();
  public:

  // @@protoc_insertion_point(class_scope:io.deephaven.proto.backplane.grpc.FetchObjectResponse)
 private:
  class _Internal;

  template <typename T> friend class ::PROTOBUF_NAMESPACE_ID::Arena::InternalHelper;
  typedef void InternalArenaConstructable_;
  typedef void DestructorSkippable_;
  ::PROTOBUF_NAMESPACE_ID::RepeatedPtrField< ::io::deephaven::proto::backplane::grpc::TypedTicket > typed_export_id_;
  ::PROTOBUF_NAMESPACE_ID::internal::ArenaStringPtr type_;
  ::PROTOBUF_NAMESPACE_ID::internal::ArenaStringPtr data_;
  mutable ::PROTOBUF_NAMESPACE_ID::internal::CachedSize _cached_size_;
  friend struct ::TableStruct_deephaven_2fproto_2fobject_2eproto;
};
// ===================================================================


// ===================================================================

#ifdef __GNUC__
  #pragma GCC diagnostic push
  #pragma GCC diagnostic ignored "-Wstrict-aliasing"
#endif  // __GNUC__
// FetchObjectRequest

// .io.deephaven.proto.backplane.grpc.TypedTicket source_id = 1;
inline bool FetchObjectRequest::_internal_has_source_id() const {
  return this != internal_default_instance() && source_id_ != nullptr;
}
inline bool FetchObjectRequest::has_source_id() const {
  return _internal_has_source_id();
}
inline const ::io::deephaven::proto::backplane::grpc::TypedTicket& FetchObjectRequest::_internal_source_id() const {
  const ::io::deephaven::proto::backplane::grpc::TypedTicket* p = source_id_;
  return p != nullptr ? *p : reinterpret_cast<const ::io::deephaven::proto::backplane::grpc::TypedTicket&>(
      ::io::deephaven::proto::backplane::grpc::_TypedTicket_default_instance_);
}
inline const ::io::deephaven::proto::backplane::grpc::TypedTicket& FetchObjectRequest::source_id() const {
  // @@protoc_insertion_point(field_get:io.deephaven.proto.backplane.grpc.FetchObjectRequest.source_id)
  return _internal_source_id();
}
inline void FetchObjectRequest::unsafe_arena_set_allocated_source_id(
    ::io::deephaven::proto::backplane::grpc::TypedTicket* source_id) {
  if (GetArenaForAllocation() == nullptr) {
    delete reinterpret_cast<::PROTOBUF_NAMESPACE_ID::MessageLite*>(source_id_);
  }
  source_id_ = source_id;
  if (source_id) {
    
  } else {
    
  }
  // @@protoc_insertion_point(field_unsafe_arena_set_allocated:io.deephaven.proto.backplane.grpc.FetchObjectRequest.source_id)
}
inline ::io::deephaven::proto::backplane::grpc::TypedTicket* FetchObjectRequest::release_source_id() {
  
  ::io::deephaven::proto::backplane::grpc::TypedTicket* temp = source_id_;
  source_id_ = nullptr;
#ifdef PROTOBUF_FORCE_COPY_IN_RELEASE
  auto* old =  reinterpret_cast<::PROTOBUF_NAMESPACE_ID::MessageLite*>(temp);
  temp = ::PROTOBUF_NAMESPACE_ID::internal::DuplicateIfNonNull(temp);
  if (GetArenaForAllocation() == nullptr) { delete old; }
#else  // PROTOBUF_FORCE_COPY_IN_RELEASE
  if (GetArenaForAllocation() != nullptr) {
    temp = ::PROTOBUF_NAMESPACE_ID::internal::DuplicateIfNonNull(temp);
  }
#endif  // !PROTOBUF_FORCE_COPY_IN_RELEASE
  return temp;
}
inline ::io::deephaven::proto::backplane::grpc::TypedTicket* FetchObjectRequest::unsafe_arena_release_source_id() {
  // @@protoc_insertion_point(field_release:io.deephaven.proto.backplane.grpc.FetchObjectRequest.source_id)
  
  ::io::deephaven::proto::backplane::grpc::TypedTicket* temp = source_id_;
  source_id_ = nullptr;
  return temp;
}
inline ::io::deephaven::proto::backplane::grpc::TypedTicket* FetchObjectRequest::_internal_mutable_source_id() {
  
  if (source_id_ == nullptr) {
    auto* p = CreateMaybeMessage<::io::deephaven::proto::backplane::grpc::TypedTicket>(GetArenaForAllocation());
    source_id_ = p;
  }
  return source_id_;
}
inline ::io::deephaven::proto::backplane::grpc::TypedTicket* FetchObjectRequest::mutable_source_id() {
  ::io::deephaven::proto::backplane::grpc::TypedTicket* _msg = _internal_mutable_source_id();
  // @@protoc_insertion_point(field_mutable:io.deephaven.proto.backplane.grpc.FetchObjectRequest.source_id)
  return _msg;
}
inline void FetchObjectRequest::set_allocated_source_id(::io::deephaven::proto::backplane::grpc::TypedTicket* source_id) {
  ::PROTOBUF_NAMESPACE_ID::Arena* message_arena = GetArenaForAllocation();
  if (message_arena == nullptr) {
    delete reinterpret_cast< ::PROTOBUF_NAMESPACE_ID::MessageLite*>(source_id_);
  }
  if (source_id) {
    ::PROTOBUF_NAMESPACE_ID::Arena* submessage_arena =
        ::PROTOBUF_NAMESPACE_ID::Arena::InternalHelper<
            ::PROTOBUF_NAMESPACE_ID::MessageLite>::GetOwningArena(
                reinterpret_cast<::PROTOBUF_NAMESPACE_ID::MessageLite*>(source_id));
    if (message_arena != submessage_arena) {
      source_id = ::PROTOBUF_NAMESPACE_ID::internal::GetOwnedMessage(
          message_arena, source_id, submessage_arena);
    }
    
  } else {
    
  }
  source_id_ = source_id;
  // @@protoc_insertion_point(field_set_allocated:io.deephaven.proto.backplane.grpc.FetchObjectRequest.source_id)
}

// -------------------------------------------------------------------

// FetchObjectResponse

// string type = 1;
inline void FetchObjectResponse::clear_type() {
  type_.ClearToEmpty();
}
inline const std::string& FetchObjectResponse::type() const {
  // @@protoc_insertion_point(field_get:io.deephaven.proto.backplane.grpc.FetchObjectResponse.type)
  return _internal_type();
}
template <typename ArgT0, typename... ArgT>
inline PROTOBUF_ALWAYS_INLINE
void FetchObjectResponse::set_type(ArgT0&& arg0, ArgT... args) {
 
 type_.Set(::PROTOBUF_NAMESPACE_ID::internal::ArenaStringPtr::EmptyDefault{}, static_cast<ArgT0 &&>(arg0), args..., GetArenaForAllocation());
  // @@protoc_insertion_point(field_set:io.deephaven.proto.backplane.grpc.FetchObjectResponse.type)
}
inline std::string* FetchObjectResponse::mutable_type() {
  std::string* _s = _internal_mutable_type();
  // @@protoc_insertion_point(field_mutable:io.deephaven.proto.backplane.grpc.FetchObjectResponse.type)
  return _s;
}
inline const std::string& FetchObjectResponse::_internal_type() const {
  return type_.Get();
}
inline void FetchObjectResponse::_internal_set_type(const std::string& value) {
  
  type_.Set(::PROTOBUF_NAMESPACE_ID::internal::ArenaStringPtr::EmptyDefault{}, value, GetArenaForAllocation());
}
inline std::string* FetchObjectResponse::_internal_mutable_type() {
  
  return type_.Mutable(::PROTOBUF_NAMESPACE_ID::internal::ArenaStringPtr::EmptyDefault{}, GetArenaForAllocation());
}
inline std::string* FetchObjectResponse::release_type() {
  // @@protoc_insertion_point(field_release:io.deephaven.proto.backplane.grpc.FetchObjectResponse.type)
  return type_.Release(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArenaForAllocation());
}
inline void FetchObjectResponse::set_allocated_type(std::string* type) {
  if (type != nullptr) {
    
  } else {
    
  }
  type_.SetAllocated(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), type,
      GetArenaForAllocation());
  // @@protoc_insertion_point(field_set_allocated:io.deephaven.proto.backplane.grpc.FetchObjectResponse.type)
}

// bytes data = 2;
inline void FetchObjectResponse::clear_data() {
  data_.ClearToEmpty();
}
inline const std::string& FetchObjectResponse::data() const {
  // @@protoc_insertion_point(field_get:io.deephaven.proto.backplane.grpc.FetchObjectResponse.data)
  return _internal_data();
}
template <typename ArgT0, typename... ArgT>
inline PROTOBUF_ALWAYS_INLINE
void FetchObjectResponse::set_data(ArgT0&& arg0, ArgT... args) {
 
 data_.SetBytes(::PROTOBUF_NAMESPACE_ID::internal::ArenaStringPtr::EmptyDefault{}, static_cast<ArgT0 &&>(arg0), args..., GetArenaForAllocation());
  // @@protoc_insertion_point(field_set:io.deephaven.proto.backplane.grpc.FetchObjectResponse.data)
}
inline std::string* FetchObjectResponse::mutable_data() {
  std::string* _s = _internal_mutable_data();
  // @@protoc_insertion_point(field_mutable:io.deephaven.proto.backplane.grpc.FetchObjectResponse.data)
  return _s;
}
inline const std::string& FetchObjectResponse::_internal_data() const {
  return data_.Get();
}
inline void FetchObjectResponse::_internal_set_data(const std::string& value) {
  
  data_.Set(::PROTOBUF_NAMESPACE_ID::internal::ArenaStringPtr::EmptyDefault{}, value, GetArenaForAllocation());
}
inline std::string* FetchObjectResponse::_internal_mutable_data() {
  
  return data_.Mutable(::PROTOBUF_NAMESPACE_ID::internal::ArenaStringPtr::EmptyDefault{}, GetArenaForAllocation());
}
inline std::string* FetchObjectResponse::release_data() {
  // @@protoc_insertion_point(field_release:io.deephaven.proto.backplane.grpc.FetchObjectResponse.data)
  return data_.Release(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArenaForAllocation());
}
inline void FetchObjectResponse::set_allocated_data(std::string* data) {
  if (data != nullptr) {
    
  } else {
    
  }
  data_.SetAllocated(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), data,
      GetArenaForAllocation());
  // @@protoc_insertion_point(field_set_allocated:io.deephaven.proto.backplane.grpc.FetchObjectResponse.data)
}

// repeated .io.deephaven.proto.backplane.grpc.TypedTicket typed_export_id = 3;
inline int FetchObjectResponse::_internal_typed_export_id_size() const {
  return typed_export_id_.size();
}
inline int FetchObjectResponse::typed_export_id_size() const {
  return _internal_typed_export_id_size();
}
inline ::io::deephaven::proto::backplane::grpc::TypedTicket* FetchObjectResponse::mutable_typed_export_id(int index) {
  // @@protoc_insertion_point(field_mutable:io.deephaven.proto.backplane.grpc.FetchObjectResponse.typed_export_id)
  return typed_export_id_.Mutable(index);
}
inline ::PROTOBUF_NAMESPACE_ID::RepeatedPtrField< ::io::deephaven::proto::backplane::grpc::TypedTicket >*
FetchObjectResponse::mutable_typed_export_id() {
  // @@protoc_insertion_point(field_mutable_list:io.deephaven.proto.backplane.grpc.FetchObjectResponse.typed_export_id)
  return &typed_export_id_;
}
inline const ::io::deephaven::proto::backplane::grpc::TypedTicket& FetchObjectResponse::_internal_typed_export_id(int index) const {
  return typed_export_id_.Get(index);
}
inline const ::io::deephaven::proto::backplane::grpc::TypedTicket& FetchObjectResponse::typed_export_id(int index) const {
  // @@protoc_insertion_point(field_get:io.deephaven.proto.backplane.grpc.FetchObjectResponse.typed_export_id)
  return _internal_typed_export_id(index);
}
inline ::io::deephaven::proto::backplane::grpc::TypedTicket* FetchObjectResponse::_internal_add_typed_export_id() {
  return typed_export_id_.Add();
}
inline ::io::deephaven::proto::backplane::grpc::TypedTicket* FetchObjectResponse::add_typed_export_id() {
  ::io::deephaven::proto::backplane::grpc::TypedTicket* _add = _internal_add_typed_export_id();
  // @@protoc_insertion_point(field_add:io.deephaven.proto.backplane.grpc.FetchObjectResponse.typed_export_id)
  return _add;
}
inline const ::PROTOBUF_NAMESPACE_ID::RepeatedPtrField< ::io::deephaven::proto::backplane::grpc::TypedTicket >&
FetchObjectResponse::typed_export_id() const {
  // @@protoc_insertion_point(field_list:io.deephaven.proto.backplane.grpc.FetchObjectResponse.typed_export_id)
  return typed_export_id_;
}

#ifdef __GNUC__
  #pragma GCC diagnostic pop
#endif  // __GNUC__
// -------------------------------------------------------------------


// @@protoc_insertion_point(namespace_scope)

}  // namespace grpc
}  // namespace backplane
}  // namespace proto
}  // namespace deephaven
}  // namespace io

// @@protoc_insertion_point(global_scope)

#include <google/protobuf/port_undef.inc>
#endif  // GOOGLE_PROTOBUF_INCLUDED_GOOGLE_PROTOBUF_INCLUDED_deephaven_2fproto_2fobject_2eproto