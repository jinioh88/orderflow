import type { Metadata } from "next";
import { ProductsView } from "@/features/catalog/components/products-view";

export const metadata: Metadata = { title: "상품" };

export default function ProductsPage() {
  return <ProductsView />;
}
